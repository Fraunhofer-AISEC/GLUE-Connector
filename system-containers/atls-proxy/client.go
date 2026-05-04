package main

import (
	"crypto/tls"
	"crypto/x509"
	"encoding/json"
	"fmt"
	"io"
	"log/slog"
	"net"
	"net/http"
	"net/url"
	"slices"
	"strconv"
	"sync"

	"github.com/Fraunhofer-AISEC/cmc/attestationreport"
	"github.com/Fraunhofer-AISEC/cmc/attestedtls"
)

type http2Conn struct {
	r io.ReadCloser
	w http.ResponseWriter
}

func (c *http2Conn) Read(b []byte) (int, error) {
	return c.r.Read(b)
}

func (c *http2Conn) Write(b []byte) (int, error) {
	return c.w.Write(b)
}

func (c *http2Conn) Close() error {
	return c.r.Close()
}

type containerPort struct {
	ContainerPort uint16 `json:"containerPort"`
	HostAddress   string `json:"hostAddress"`
	Protocol      string `json:"protocol"`
}

type remotePipelineStatus struct {
	Id       string          `json:"id"`
	RemoteId string          `json:"remoteId"`
	Peer     string          `json:"peer"`
	Ports    []containerPort `json:"ports"`
}

type client struct {
	config      config
	proxyConfig atlsProxyConfig

	tlsConfig *tls.Config
	dial      func(string) (net.Conn, error)
}

func (c *client) selectPeerEndpoint(ports []containerPort, selected string) (string, error) {
	var index int
	if selected != "" {
		selectedInt, err := strconv.ParseUint(selected, 10, 16)
		if err != nil {
			return "", fmt.Errorf("could not parse selected port: %w", err)
		}

		index = slices.IndexFunc(ports, func(port containerPort) bool {
			return port.ContainerPort == uint16(selectedInt)
		})
	} else {
		index = slices.IndexFunc(ports, func(port containerPort) bool {
			return port.Protocol == "tcp"
		})
	}

	if index == -1 {
		return "", fmt.Errorf("could not find an appropriate container port")
	}

	return ports[index].HostAddress, nil
}

func (c *client) negotiateContract(destination string, action string, port string) (string, error) {
	var actionFragment string
	if action != "" {
		actionFragment = fmt.Sprintf("&action=%s", url.QueryEscape(action))
	}

	coordinatorUrl := fmt.Sprintf(
		"http://%s:8080/coordinator/negotiate?destination=%s%s",
		c.config.Controller,
		url.QueryEscape(destination),
		actionFragment,
	)

	resp, err := http.Post(coordinatorUrl, "application/json", nil)
	if err != nil {
		return "", fmt.Errorf("error contacting the coordinator: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode < 200 || resp.StatusCode > 299 {
		return "", fmt.Errorf("got error response from coordinator: %s", resp.Status)
	}

	var pipeline remotePipelineStatus
	if err := json.NewDecoder(resp.Body).Decode(&pipeline); err != nil {
		return "", fmt.Errorf("could not decode the negotiation response from the coordinator: %w", err)
	}

	return c.selectPeerEndpoint(pipeline.Ports, port)
}

func (c *client) dialAtls(peerAddr string) (net.Conn, error) {
	serializer, err := attestationreport.NewJsonSerializer()
	if err != nil {
		slog.Error("Failed to set up connection", "error", fmt.Errorf("could not create a json serializer: %w", err))
		return nil, err
	}

	return attestedtls.Dial(
		"tcp",
		peerAddr,
		c.tlsConfig,
		attestedtls.WithCmcAddr(c.proxyConfig.CmcAddress),
		attestedtls.WithAttest(attestedtls.Attest_Mutual),
		attestedtls.WithCmcApi("socket"),
		attestedtls.WithMtls(true),
		attestedtls.WithSerializer(serializer),
		attestedtls.WithResultCb(func(result *attestationreport.AttestationResult) {
			switch result.Summary.Status {
			case attestationreport.StatusSuccess:
				slog.Info("Verification for Prover", "prover", result.Prover, "created", result.Created, "status", result.Summary.Status)
			default:
				slog.Warn("Verification for Prover", "prover", result.Prover, "created", result.Created, "status", result.Summary.Status)
				result.PrintErr()
			}
		}),
	)
}

func (c *client) dialPlain(peerAddr string) (net.Conn, error) {
	return net.Dial("tcp", peerAddr)
}

func (c *client) proxy(upstream, downstream io.ReadWriteCloser) {
	var wg sync.WaitGroup
	forward := func(from io.Reader, to io.Writer, direction string) {
		defer wg.Done()
		_, err := io.Copy(to, from)
		if cw, ok := to.(interface{ CloseWrite() error }); ok {
			cw.CloseWrite()
		}
		if err != nil {
			slog.Error("Error forwarding data", "direction", direction, "error", err)
		}
	}

	wg.Add(2)
	go forward(upstream, downstream, "forward")
	go forward(downstream, upstream, "backward")
	wg.Wait()
}

func (c *client) httpHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodConnect {
		http.Error(w, "This proxy only supports HTTP CONNECT requests", http.StatusMethodNotAllowed)
		return
	}
	slog.Debug("Incoming HTTP CONNECT request", "source", r.RemoteAddr, "destination", r.URL)

	if r.URL.Port() == "" {
		http.Error(w, "No port provided", http.StatusBadRequest)
		return
	}

	glueAction := r.Header.Get("X-GLUE-Action")
	gluePort := r.Header.Get("X-GLUE-Port")

	peerAddr, err := c.negotiateContract(r.URL.Host, glueAction, gluePort)
	if err != nil {
		errString := fmt.Sprintf("Error in the control plane: %s", err)
		http.Error(w, errString, http.StatusBadGateway)
	}

	downstream, err := c.dial(peerAddr)
	if err != nil {
		errString := fmt.Sprintf("Could not dial the downstream connection: %s", err)
		http.Error(w, errString, http.StatusBadGateway)
	}
	defer downstream.Close()

	w.WriteHeader(http.StatusOK)
	if f, ok := w.(http.Flusher); ok {
		f.Flush()
	}

	var upstream io.ReadWriteCloser
	if h, ok := w.(http.Hijacker); ok {
		conn, buffered, err := h.Hijack()
		if err != nil {
			// We have already written the OK response to the caller, so we cannot do proper error reporting anymore
			return
		}

		// The error can be ignored since we peek the exact length of the buffer
		buf, _ := buffered.Reader.Peek(buffered.Reader.Buffered())
		n, err := downstream.Write(buf)
		if err != nil {
			return
		}
		if n != len(buf) {
			return
		}

		upstream = conn
	} else {
		upstream = &http2Conn{
			r: r.Body,
			w: w,
		}
	}
	defer upstream.Close()

	c.proxy(upstream, downstream)
}

// clientMain is the entry point for client-mode ATLS proxy
// It creates a listener for each configured connection that accepts incoming connections
// and establishes ATLS connections to the next hop after coordinator negotiation
func clientMain(config config, proxyConfig atlsProxyConfig) error {
	tlsConfig, err := createClientTLSConfig(proxyConfig)
	if err != nil {
		return err
	}

	client := client{
		config:      config,
		proxyConfig: proxyConfig,

		tlsConfig: tlsConfig,
	}

	client.dial = client.dialAtls

	slog.Info("Accepting HTTP CONNECT requests", "address", "0.0.0.0:8080")
	return http.ListenAndServe("0.0.0.0:8080", http.HandlerFunc(client.httpHandler))
}

// createClientTLSConfig initializes the TLS configuration for client connections
func createClientTLSConfig(proxyConfig atlsProxyConfig) (*tls.Config, error) {
	caCerts := x509.NewCertPool()
	caCerts.AppendCertsFromPEM([]byte(proxyConfig.CaCertificates))
	serializer, err := attestationreport.NewJsonSerializer()
	if err != nil {
		return nil, fmt.Errorf("could not create a json serializer: %w", err)
	}

	clientCert, err := attestedtls.GetCert(
		attestedtls.WithCmcAddr(proxyConfig.CmcAddress),
		attestedtls.WithCmcApi("socket"),
		attestedtls.WithSerializer(serializer),
		attestedtls.WithKeyId(proxyConfig.KeyId),
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
