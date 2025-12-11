package main

import (
	"context"
	"crypto/tls"
	"crypto/x509"
	"fmt"
	"log/slog"
	"net"
	"time"

	"github.com/Fraunhofer-AISEC/cmc/attestationreport"
	"github.com/Fraunhofer-AISEC/cmc/attestedtls"
)

const (
	serverListenAddr  = "0.0.0.0:8080"
	nextContainerPort = 8080
	dialRetries       = 5
	dialRetryDelay    = 5 * time.Second
)

// ServerProxy manages the server-side proxy that accepts ATLS connections
// and forwards them to the next container in the pipeline
type ServerProxy struct {
	ctx         context.Context
	config      config
	proxyConfig atlsProxyConfig
	tlsConfig   *tls.Config
	nextAddr    string
}

// dialNextContainer attempts to connect to the next container with retries
func (sp *ServerProxy) dialNextContainer(clientAddr net.Addr) (net.Conn, error) {
	var conn net.Conn

	for i := dialRetries; i > 0; i-- {
		var err error
		conn, err = net.Dial("tcp", sp.nextAddr)
		if err == nil {
			return conn, nil
		}

		slog.Error("Failed to connect to the next address",
			"error", err,
			"triesLeft", i-1,
			"delay", dialRetryDelay,
			"clientAddr", clientAddr)

		if i > 1 {
			time.Sleep(dialRetryDelay)
		}
	}

	return nil, fmt.Errorf("maximum tries exceeded when connecting to the next container")
}

// handleConnection processes a single ATLS client connection
func (sp *ServerProxy) handleConnection(clientConn net.Conn) {
	defer clientConn.Close()

	serverConn, err := sp.dialNextContainer(clientConn.RemoteAddr())
	if err != nil {
		slog.Error("Could not connect to next container",
			"error", err,
			"clientAddr", clientConn.RemoteAddr())
		return
	}

	proxy(sp.ctx, clientConn, serverConn)
}

// listen accepts ATLS connections and forwards them to the next container
func (sp *ServerProxy) listen() error {
	listener, err := attestedtls.Listen(
		"tcp",
		serverListenAddr,
		sp.tlsConfig,
		attestedtls.WithCmcAddr(sp.proxyConfig.CmcAddress),
		attestedtls.WithAttest(attestedtls.Attest_Mutual),
		attestedtls.WithCmcApi(attestedtls.CmcApi_Socket),
		attestedtls.WithMtls(true),
		attestedtls.WithApiSerializer(attestationreport.JsonSerializer{}),
	)
	if err != nil {
		return fmt.Errorf("failed to listen for ATLS connections on %s: %w", serverListenAddr, err)
	}
	defer listener.Close()

	slog.Info("Listening for ATLS connections",
		"listenAddress", serverListenAddr,
		"nextHop", sp.config.NextContainer)

	go func() {
		<-sp.ctx.Done()
		listener.Close()
	}()

	for {
		clientConn, err := listener.Accept()
		if err != nil {
			slog.Error("Failed to accept client connection",
				"listenAddress", serverListenAddr,
				"nextHop", sp.config.NextContainer,
				"error", err)
			continue
		}

		go sp.handleConnection(clientConn)
	}
}

// serverMain is the entry point for server-mode ATLS proxy
// It accepts ATLS connections and forwards them to the next container
func serverMain(config config, proxyConfig atlsProxyConfig) error {
	tlsConfig, err := createServerTLSConfig(proxyConfig)
	if err != nil {
		return err
	}

	proxy := &ServerProxy{
		ctx:         context.Background(),
		config:      config,
		proxyConfig: proxyConfig,
		tlsConfig:   tlsConfig,
		nextAddr:    fmt.Sprintf("%s:%d", config.NextContainer, nextContainerPort),
	}

	return proxy.listen()
}

// createServerTLSConfig initializes the TLS configuration for server connections
func createServerTLSConfig(proxyConfig atlsProxyConfig) (*tls.Config, error) {
	caCerts := x509.NewCertPool()
	caCerts.AppendCertsFromPEM([]byte(proxyConfig.CaCertificates))

	serverCert, err := attestedtls.GetCert(
		attestedtls.WithCmcAddr(proxyConfig.CmcAddress),
		attestedtls.WithCmcApi(attestedtls.CmcApi_Socket),
		attestedtls.WithApiSerializer(attestationreport.JsonSerializer{}),
	)
	if err != nil {
		return nil, fmt.Errorf("failed to get the server certificate from the CMC: %w", err)
	}

	return &tls.Config{
		ClientCAs:    caCerts,
		Certificates: []tls.Certificate{serverCert},
		ClientAuth:   tls.RequireAndVerifyClientCert,
		// TODO: Revert this once proper enrollment is done in the VMs
		InsecureSkipVerify: true,
	}, nil
}
