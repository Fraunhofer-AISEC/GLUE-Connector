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

type config struct {
	// Controller address
	Controller string `json:"controller"`
	// The address of the next controller
	NextContainer string `json:"nextContainer"`
	// The parameters for the ATLS proxy. We only care about those of type
	Parameters []json.RawMessage `json:"parameters"`
}

type proxyEndpoint struct {
	// The local endpoint to listen on for incoming connections
	ListenEndpoint string `json:"http://fhg.de/glue/listenEndpoint"`
	// The remote endpoint (next container port for server mode, peer for client mode)
	NextEndpoint string `json:"http://fhg.de/glue/nextEndpoint"`
}

type atlsProxyConfig struct {
	Type string `json:"@type"`
	// Either server or client
	Mode string `json:"http://fhg.de/glue/mode"`
	// Where the container can reach the CMC
	CmcAddress string `json:"http://fhg.de/glue/cmcAddress"`
	// PEM-encoded CA certificates to allow for ATLS connections
	CaCertificates string `json:"http://fhg.de/glue/caCertificates"`
	// The connections this instance of the ATLS proxy should handle
	ProxyEndpoints []proxyEndpoint `json:"http://fhg.de/glue/proxyEndpoint"`
}

func parseConfig() (config, atlsProxyConfig, error) {
	file, err := os.Open("/config.json")
	if err != nil {
		return config{}, atlsProxyConfig{}, fmt.Errorf("could not open config: %w", err)
	}
	defer file.Close()

	var conf config
	err = json.NewDecoder(file).Decode(&conf)
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

func copyWithTimeout(from, to net.Conn, deadline time.Duration) error {
    var buffer [10 * 1024]byte
    for {
        rn, err := from.Read(buffer[:])
        // Write before error handling in case we have somehow read some data before the error
        if rn > 0 {
            wn, err := to.Write(buffer[:rn])
            if err != nil {
                return fmt.Errorf("write error: %w", err)
            }
            if wn != rn {
                return fmt.Errorf("could not write all data to destination socket")
            }
        }
        if err != nil {
            if err != io.EOF {
                return fmt.Errorf("read error: %w", err)
            }
            return nil
        }
        _ = from.SetReadDeadline(time.Now().Add(deadline))
        _ = to.SetWriteDeadline(time.Now().Add(deadline))
    }
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

	forward := func(from, to net.Conn) {
		err := copyWithTimeout(from, to, 6 * time.Minute)
		if errors.Is(err, os.ErrDeadlineExceeded) && ctxCancelled.Load() {
			// This error is due to the context being cancelled, ignore
		} else {
			slog.Error("Error forwarding data", "from", from.RemoteAddr(), "to", to.RemoteAddr(), "error", err)
		}
	}

	var wg sync.WaitGroup
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
