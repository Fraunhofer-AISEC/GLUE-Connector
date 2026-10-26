package main

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"log/slog"
	"net"
	"os"
	"sync"
	"sync/atomic"
	"time"
)

type oneOrMany[T any] []T

func (values *oneOrMany[T]) UnmarshalJSON(data []byte) error {
	if string(data) == "null" {
		*values = nil
		return nil
	}

	var many []T
	if err := json.Unmarshal(data, &many); err == nil {
		*values = many
		return nil
	}

	var one T
	if err := json.Unmarshal(data, &one); err != nil {
		return fmt.Errorf("expected a single value or an array: %w", err)
	}

	*values = []T{one}
	return nil
}

type config struct {
	// Controller address
	Controller string `json:"controller"`
	// The address of the next controller
	NextContainer string `json:"nextContainer"`
	// The parameters for the ATLS proxy. We only care about those of type
	Parameters []json.RawMessage `json:"parameters"`
}

type proxyEndpoint struct {
    // Needed to decode this nested struct apparently
    Type string `json:"type"`
	// The local endpoint to listen on for incoming connections
	ListenEndpoint string `json:"http://fhg.de/glue/listenEndpoint"`
	// The remote endpoint (next container port for server mode, peer for client mode)
	NextEndpoint string `json:"http://fhg.de/glue/nextEndpoint"`
}

type atlsProxyConfig struct {
	Type string `json:"type"`
	// If true, ATLS is enabled globally
	Enabled bool `json:"http://fhg.de/glue/enabled"`
	// Either server or client
	Mode string `json:"http://fhg.de/glue/mode"`
	// Where the container can reach the CMC
	CmcAddress string `json:"http://fhg.de/glue/cmcAddress"`
	// PEM-encoded CA certificates to allow for ATLS connections
	CaCertificates string `json:"http://fhg.de/glue/caCertificates"`
	// The connections this instance of the ATLS proxy should handle
	ProxyEndpoints oneOrMany[proxyEndpoint] `json:"http://fhg.de/glue/proxyEndpoint"`
	// The key ID to access the CMC TLS signing key with
	KeyId string `json:"http://fhg.de/glue/keyId"`
}

func parseConfig() (config, atlsProxyConfig, error) {
	file, err := os.Open("/config.json")
	if err != nil {
		return config{}, atlsProxyConfig{}, fmt.Errorf("could not open config: %w", err)
	}
	defer file.Close()

	var conf config
	err = json.NewDecoder(io.TeeReader(file, os.Stdout)).Decode(&conf)
	if err != nil {
		return config{}, atlsProxyConfig{}, fmt.Errorf("could not parse config: %w", err)
	}

	for _, param := range conf.Parameters {
		var decodedParam atlsProxyConfig
		err := json.Unmarshal(param, &decodedParam)
		if err != nil {
			return config{}, atlsProxyConfig{}, fmt.Errorf("could not parse parameter: %w", err)
		}

		if decodedParam.Type == "http://fhg.de/glue/internal/AtlsProxyConfiguration" {
			return conf, decodedParam, nil
		}
	}

	return config{}, atlsProxyConfig{}, fmt.Errorf("atls proxy parameter is missing")
}

func proxy(ctx context.Context, conn1, conn2 net.Conn) {
	localCtx, cancel := context.WithCancel(ctx)
	defer cancel()
	var ctxCancelled atomic.Bool

	go func() {
		<-localCtx.Done()
		ctxCancelled.Store(true)
		deadline := time.Now().Add(time.Millisecond)
		// We set really small read deadlines to get the reading goroutines to wake up
		conn1.SetReadDeadline(deadline)
		conn2.SetReadDeadline(deadline)
	}()

	var wg sync.WaitGroup
	forward := func(from, to net.Conn) {
		defer wg.Done()
		_, err := io.Copy(to, from)
		if errors.Is(err, os.ErrDeadlineExceeded) && ctxCancelled.Load() {
			// This error is due to the context being canceled, ignore
		} else {
			slog.Error("Error forwarding data", "from", from.RemoteAddr(), "to", to.RemoteAddr(), "error", err)
		}
	}
	wg.Add(2)
	go forward(conn1, conn2)
	go forward(conn2, conn1)
	wg.Wait()
}

func main() {
	config, proxyConfig, err := parseConfig()
	if err != nil {
		slog.Error("Failed to parse the container configuration file", "error", err)
		os.Exit(1)
	}

	switch proxyConfig.Mode {
	case "server":
		if err := serverMain(config, proxyConfig); err != nil {
			slog.Error("Error running the ATLS proxy in server mode", "error", err)
			os.Exit(1)
		}
	case "client":
		if err := clientMain(config, proxyConfig); err != nil {
			slog.Error("Error running the ATLS proxy in client mode", "error", err)
			os.Exit(1)
		}
	default:
		slog.Error("Invalid mode", "mode", proxyConfig.Mode)
		os.Exit(1)
	}
}
