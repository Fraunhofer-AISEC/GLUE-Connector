package main

import (
	"io"
	"log"
	"net/http"
	"os"
)

func echo(w http.ResponseWriter, r *http.Request) {
	log.Printf("Handling request from %s", r.RemoteAddr)

	reader := io.TeeReader(r.Body, os.Stdout)
	_, _ = io.Copy(w, reader)
}

func main() {
	log.Printf("Starting echo server on port 8080")

	http.HandleFunc("/echo", echo)
	log.Fatal(http.ListenAndServe("0.0.0.0:8080", http.DefaultServeMux))
}
