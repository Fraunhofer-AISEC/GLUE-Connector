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
	"sync"
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
	defer closeSilent(peer)

	var wg sync.WaitGroup

	forward := func(from net.Conn, to net.Conn) {
		defer wg.Done()
		if _, err := io.Copy(to, from); err != nil {
			log.Printf("Error while forwarding data between %s and %s: %s", conn.RemoteAddr().String(), proxyAddress, err.Error())
		}
	}

	wg.Add(2)
	go forward(conn, peer)
	go forward(peer, conn)
	wg.Wait()

	log.Printf("Connection between %s and %s closed", conn.RemoteAddr().String(), proxyAddress)
}

type pipelinePort struct {
	Endpoint string `json:"endpoint"`
	Protocol string `json:"protocol"`
}

func negotiateAndProxy(conn net.Conn, coordinator string, destination string) {
	var httpClient http.Client
	defer closeSilent(conn)

	// Register the nextDestination parameter with the coordinator (control plane)

	parameterUrl := fmt.Sprintf("http://%s:8080/coordinator/policy-parameter", coordinator)
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

	coordinatorUrl := fmt.Sprintf("http://%s:8080/coordinator/negotiate?destination=%s&startPipeline=true", coordinator, destination)
	resp, err := httpClient.Post(coordinatorUrl, "application/json", nil)
	if err != nil {
		log.Printf("Error in coordinator negotiation request: %s", err)
		return
	}
	defer closeSilent(resp.Body)
	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		log.Printf("Got error response from coordinator: %s", resp.Status)
		return
	}
	log.Printf("Got response from coordinator: %s", resp.Status)

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
	log.Printf("Using endpoint %s", endpoint)

	proxy(conn, endpoint)
}

type restParameter struct {
    Type string `json:"type"`
    HttpUrl string `json:"http://fhg.de/glue/httpUrl"`
}

type config struct {
	// Controller address
	Controller string `json:"controller"`
	// The address of the next controller
	NextContainer string `json:"nextContainer"`
	// Arbitrary parameters
	Parameters []json.RawMessage `json:"parameters"`
}

func main() {
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

	var nextHop string
	for _, param := range config.Parameters {
	    var decodedParam restParameter
	    err := json.Unmarshal(param, &decodedParam)
	    if err != nil {
	        log.Fatalf("Could not deserialize config param: %s", err)
	    }

        if decodedParam.Type != "http://fhg.de/glue/RestParameter" {
            log.Printf("Discarding unused parameter %s", decodedParam.Type)
            continue
        }

        log.Printf("Setting next hop address to %s", nextHop)
        nextHop = decodedParam.HttpUrl
        break
	}

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

		go negotiateAndProxy(conn, config.Controller, nextHop)
	}
}
