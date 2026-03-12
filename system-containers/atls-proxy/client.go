package main

import (
	"context"
	"crypto/tls"
	"crypto/x509"
	"encoding/json"
	"fmt"
	"log/slog"
	"net"
	"net/http"
	"sync"
	"time"

	"github.com/Fraunhofer-AISEC/cmc/attestationreport"
	"github.com/Fraunhofer-AISEC/cmc/attestedtls"
	"golang.org/x/sync/errgroup"
)

// ClientProxy manages a single client-side proxy connection
type ClientProxy struct {
	ctx              context.Context
	config           config
	proxyConfig      atlsProxyConfig
	connectionConfig proxyEndpoint
	tlsConfig        *tls.Config

	// For triggering the follow-up negotiation
	setupOnce sync.Once
	peerAddr  string
	setupErr  error
}

// negotiateWithCoordinator triggers the follow-up negotiation with the next connector in the pipeline
func (cp *ClientProxy) negotiateWithCoordinator() error {
	coordinatorUrl := fmt.Sprintf(
		"http://%s:8080/coordinator/negotiate?destination=http://%s&startPipeline=true",
		cp.config.Controller,
		cp.connectionConfig.NextEndpoint,
	)

	resp, err := http.Post(coordinatorUrl, "application/json", nil)
	if err != nil {
		return fmt.Errorf("error contacting the coordinator: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode < 200 || resp.StatusCode > 299 {
		return fmt.Errorf("got error response from coordinator: %s", resp.Status)
	}

	var endpoints []struct {
		Endpoint string `json:"endpoint"`
		Protocol string `json:"protocol"`
	}

	if err := json.NewDecoder(resp.Body).Decode(&endpoints); err != nil {
		return fmt.Errorf("could not decode the negotiation response from the coordinator: %w", err)
	}

	peerAddr := cp.selectPeerEndpoint(endpoints)
	if peerAddr == "" {
		return fmt.Errorf("the coordinator did not return any suitable port mapping for the next pipeline")
	}

	cp.peerAddr = peerAddr
	return nil
}

// selectPeerEndpoint chooses the best endpoint from the coordinator response
// Prefers port 8080/TCP, then falls back to the first available TCP endpoint
func (cp *ClientProxy) selectPeerEndpoint(endpoints []struct {
	Endpoint string `json:"endpoint"`
	Protocol string `json:"protocol"`
}) string {
	var fallback string

	for _, endpoint := range endpoints {
		_, port, err := net.SplitHostPort(endpoint.Endpoint)
		if err != nil {
			slog.Warn("Received invalid endpoint from coordinator", "endpoint", endpoint.Endpoint)
			continue
		}

		if endpoint.Protocol == "TCP" {
			if port == "8080" {
				return endpoint.Endpoint
			}
			if fallback == "" {
				fallback = endpoint.Endpoint
			}
		}
	}

	return fallback
}

// ensureSetup makes sure that the follow-up negotiation is performed once
func (cp *ClientProxy) ensureSetup() error {
	cp.setupOnce.Do(func() {
		cp.setupErr = cp.negotiateWithCoordinator()
	})
	return cp.setupErr
}

// handleConnection processes a single client connection by establishing an ATLS connection to the peer
func (cp *ClientProxy) handleConnection(clientConn net.Conn) {
	defer clientConn.Close()

	if err := cp.ensureSetup(); err != nil {
		slog.Error("Failed to setup connection", "error", err)
		return
	}

	atlsConn, err := attestedtls.Dial(
		"tcp",
		cp.peerAddr,
		cp.tlsConfig,
		attestedtls.WithCmcAddr(cp.proxyConfig.CmcAddress),
		attestedtls.WithAttest(attestedtls.Attest_Mutual),
		attestedtls.WithCmcApi("socket"),
		attestedtls.WithMtls(true),
		attestedtls.WithApiSerializer(attestationreport.JsonSerializer{}),
		attestedtls.WithResultCb(func(result *attestationreport.VerificationResult) {
			switch result.Summary.Status {
			case attestationreport.StatusSuccess:
				slog.Info("SUCCESS: Verification for Prover %v (%v)", result.Prover, result.Created)
			case attestationreport.StatusWarn:
				slog.Warn("WARN: Verification for Prover %v (%v)", result.Prover, result.Created)
			default:
				slog.Warn("FAILED: Verification for Prover %v (%v)", result.Prover, result.Created)
				result.PrintErr()
			}
		}),
	)
	if err != nil {
		slog.Error("Could not open connection to the peer", "error", err)
		return
	}

	// Disable timeouts; the data apps should ensure that connections are cleaned up
	clientConn.SetDeadline(time.Time{})
	atlsConn.SetDeadline(time.Time{})

	proxy(cp.ctx, clientConn, atlsConn)
}

// listen accepts incoming connections and handles them
func (cp *ClientProxy) listen() error {
	listenAddr := cp.connectionConfig.ListenEndpoint
	listener, err := net.Listen("tcp", listenAddr)
	if err != nil {
		return fmt.Errorf("failed to listen for connections on %s: %w", listenAddr, err)
	}
	defer listener.Close()

	slog.Info("Listening for connections",
		"listenAddress", listenAddr,
		"nextConnector", cp.connectionConfig.NextEndpoint)

	go func() {
		<-cp.ctx.Done()
		listener.Close()
	}()

	for {
		clientConn, err := listener.Accept()
		if err != nil {
			slog.Error("Failed to accept client connection",
				"listenAddress", listenAddr,
				"nextConnector", cp.connectionConfig.NextEndpoint,
				"error", err)
			continue
		}

		go cp.handleConnection(clientConn)
	}
}

// clientMain is the entry point for client-mode ATLS proxy
// It creates a listener for each configured connection that accepts incoming connections
// and establishes ATLS connections to the next hop after coordinator negotiation
func clientMain(config config, proxyConfig atlsProxyConfig) error {
	group, ctx := errgroup.WithContext(context.Background())

	tlsConfig, err := createClientTLSConfig(proxyConfig)
	if err != nil {
		return err
	}

	slog.Info("Starting client listeners", "count", len(proxyConfig.ProxyEndpoints))

	for _, connConfig := range proxyConfig.ProxyEndpoints {
		proxy := &ClientProxy{
			ctx:              ctx,
			config:           config,
			proxyConfig:      proxyConfig,
			connectionConfig: connConfig,
			tlsConfig:        tlsConfig,
		}
		group.Go(proxy.listen)
	}

	return group.Wait()
}

// createClientTLSConfig initializes the TLS configuration for client connections
func createClientTLSConfig(proxyConfig atlsProxyConfig) (*tls.Config, error) {
	caCerts := x509.NewCertPool()
	caCerts.AppendCertsFromPEM([]byte(proxyConfig.CaCertificates))

	clientCert, err := attestedtls.GetCert(
		attestedtls.WithCmcAddr(proxyConfig.CmcAddress),
		attestedtls.WithCmcApi("socket"),
		attestedtls.WithApiSerializer(attestationreport.JsonSerializer{}),
	)
	if err != nil {
		return nil, fmt.Errorf("failed to get the client certificate from the CMC: %w", err)
	}

	return &tls.Config{
		RootCAs:      caCerts,
		Certificates: []tls.Certificate{clientCert},
		// TODO: Revert this once proper enrollment is done in the VMs
		InsecureSkipVerify: true,
	}, nil
}
