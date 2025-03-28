package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"net/url"
	"os"
	"strings"
)

func closeSilent(closeable io.Closer) {
	_ = closeable.Close()
}

// Join a base url with a path, ensuring that a single slash is in between
func joinUrl(base string, suffix string) string {
	trailingSlash := strings.HasSuffix(base, "/")
	leadingSlash := strings.HasPrefix(suffix, "/")

	// No slash
	if !trailingSlash && !leadingSlash {
		return base + "/" + suffix
	}
	// One slash
	if trailingSlash {
		return base + suffix
	}
	// two slashes
	return base + suffix[1:]
}

func makeHandler(connectorHost string, destination string) http.HandlerFunc {
	httpClient := http.Client{}

	// Set up the parameter for the next connector's assignee here
	// for simplicity, this is just the full destination URI
	parameterUrl := fmt.Sprintf("http://%s:8080/coordinator/policy-parameter", connectorHost)
	var paramsBuffer bytes.Buffer
	_ = json.NewEncoder(&paramsBuffer).Encode(map[string]string{"nextDataReceiverUid": destination})
	resp, err := httpClient.Post(parameterUrl, "application/json", &paramsBuffer)
	if err != nil {
		log.Fatalf("Error posting policy parameters: %s", err)
	}
	defer closeSilent(resp.Body)
	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		log.Fatalf("Got error response from coordinator: %s", resp.Status)
	}

	return func(w http.ResponseWriter, r *http.Request) {
		fullDestination := joinUrl(destination, r.URL.Path)
		proxyUrl := fmt.Sprintf("http://%s:8080/coordinator/proxy?destination=%s", connectorHost, url.QueryEscape(fullDestination))

		// This may panic if the content type header is missing...
		resp, err := httpClient.Post(proxyUrl, r.Header["Content-Type"][0], r.Body)
		if err != nil {
			log.Fatalf("Error handling request for %s: %s", r.URL, err)
		}
		defer closeSilent(resp.Body)
		if resp.StatusCode < 200 || resp.StatusCode >= 300 {
			log.Fatalf("Got error response from coordinator: %s", resp.Status)
		}

		w.WriteHeader(http.StatusOK)
		_, _ = io.Copy(w, resp.Body)
	}
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

	handler := makeHandler(config.Controller, os.Args[1])
	_ = http.ListenAndServe("0.0.0.0:8080", handler)
}
