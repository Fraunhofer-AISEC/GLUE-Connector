package main

import (
	"io"
	"log"
	"net/http"
)

func echo(w http.ResponseWriter, r *http.Request) {
	log.Printf("Handling request from %s", r.RemoteAddr)

	io.Copy(w, r.Body)
}

func main() {
	log.Printf("Starting echo server on port 8080")

	http.HandleFunc("/echo", echo)
	log.Fatal(http.ListenAndServe("0.0.0.0:8080", http.DefaultServeMux))
}
