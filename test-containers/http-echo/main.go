package main

import (
	"bytes"
	"io"
	"log"
	"net/http"
	"os"
)

func echo(w http.ResponseWriter, r *http.Request) {
	log.Printf("Handling request from %s", r.RemoteAddr)
	http.NewResponseController(w).EnableFullDuplex()

	var body bytes.Buffer
	if _, err := io.Copy(&body, io.TeeReader(r.Body, os.Stdout)); err != nil {
		log.Printf("failed to read request body: %v", err)
		return
	}

	if _, err := io.Copy(w, &body); err != nil {
		log.Printf("failed to write response body: %v", err)
	}
}

func main() {
	log.Printf("Starting echo server on port 8080")

	http.HandleFunc("/echo", echo)
	log.Fatal(http.ListenAndServe("0.0.0.0:8080", http.DefaultServeMux))
}
