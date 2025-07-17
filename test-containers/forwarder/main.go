package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net"
	"net/http"
	"os"
)

func closeSilent(closeable io.Closer) {
	_ = closeable.Close()
}

func proxy(conn net.Conn, proxyAddress string) {
	peer, err := net.Dial("tcp", proxyAddress)
	if err != nil {
		log.Printf("Could not connect to proxy address: %s", err.Error())
		return
	}
	defer peer.Close()

	forward := func(from net.Conn, to net.Conn, errCh chan<- error) {
		_, err := io.Copy(to, from)
		errCh <- err
	}

	errCh := make(chan error)

	go forward(conn, peer, errCh)
	go forward(peer, conn, errCh)

	err = <-errCh
	if err != nil {
		log.Printf("Error while forwarding data between %s and %s: %s", conn.RemoteAddr().String(), proxyAddress, err.Error())
	} else {
		log.Printf("Connection between %s and %s closed", conn.RemoteAddr().String(), proxyAddress)
	}
}

type pipelinePort struct {
	Endpoint string `json:"endpoint"`
	Protocol string `json:"protocol"`
}

func negotiateAndProxy(conn net.Conn, coordiantor string, destination string) {
	var httpClient http.Client
	defer closeSilent(conn)

	// Register the dnextDestionation parameter with the coordinator (control plane)

	parameterUrl := fmt.Sprintf("http://%s:8080/coordinator/policy-parameter", coordiantor)
	var paramBuffer bytes.Buffer
	err := json.NewEncoder(&paramBuffer).Encode(map[string]string{"nextDataReceiverUid": destination})
	if err != nil {
		log.Printf("Could not encode parameter array: %s", err)
		return
	}

	paramResp, err := httpClient.Post(parameterUrl, "application/json", &paramBuffer)
	if err != nil {
		log.Printf("Error in coordinator param request: %s", err)
		return
	}
	defer closeSilent(paramResp.Body)
	if paramResp.StatusCode < 200 || paramResp.StatusCode >= 300 {
		log.Printf("Got error response from coordinator param endpoint: %s", paramResp.Status)
		return
	}

	// Kick of the negotiation on the coordinator

	coordiantorUrl := fmt.Sprintf("http://%s:8080/coordinator/negotiate?destination=%s&startPipeline=true", coordiantor, destination)
	resp, err := httpClient.Post(coordiantorUrl, "application/json", nil)
	if err != nil {
		log.Printf("Error in coordinator negotiation request: %s", err)
		return
	}
	defer closeSilent(resp.Body)
	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		log.Printf("Got error response from coordinator: %s", resp.Status)
		return
	}

	// We get back all of the ports opened by the remote pipeline
	// Look for one that we want

	var ports []pipelinePort
	err = json.NewDecoder(resp.Body).Decode(&ports)
	if err != nil {
		log.Printf("Could not decode negotiation json object: %s", err)
		return
	}

	var endpoint string
	for _, port := range ports {
		if port.Protocol == "TCP" {
			endpoint = port.Endpoint
		}
	}

	if endpoint == "" {
		log.Printf("No candidate endpoint found, closing")
		return
	}

	proxy(conn, endpoint)
}

type config struct {
	// Controller address
	Controller string `json:"controller"`
	// The address of the next controller
	NextContainer string `json:"nextContainer"`
}

func main() {
	if len(os.Args) != 2 {
		log.Fatalf("Usage: %s <destination url>", os.Args[0])
	}

	configFile, err := os.Open("/config.json")
	if err != nil {
		log.Fatalf("Could not open the config file: %s", err)
	}

	var config config
	err = json.NewDecoder(configFile).Decode(&config)
	if err != nil {
		log.Fatalf("Could not parse config: %s", err)
	}
	log.Printf("Config: %+v", config)
	log.Printf("Next hop is at %s", os.Args[1])

	listener, err := net.Listen("tcp", "0.0.0.0:8080")
	if err != nil {
		log.Fatalf("Could not start listener on port 8080: %s", err)
	}

	for {
		conn, err := listener.Accept()
		if err != nil {
			log.Printf("Error during accept: %s", err)
			continue
		}

		log.Printf("Accepted connection from %s", conn.RemoteAddr())

		go negotiateAndProxy(conn, config.Controller, os.Args[1])
	}
}
