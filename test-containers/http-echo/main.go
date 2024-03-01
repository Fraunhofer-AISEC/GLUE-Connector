package main

import (
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
)

func echo(w http.ResponseWriter, r *http.Request) {
	log.Printf("Handling request from %s", r.RemoteAddr)

	io.Copy(w, r.Body)
}

func main() {
    if len(os.Args) != 2 {
		fmt.Fprintf(os.Stderr, "Usage: %0 <listen address>")
	}

	listenAddress := os.Args[1]
	log.Printf("Starting echo server on %s", listenAddress)

	http.HandleFunc("/echo", echo)
	log.Fatal(http.ListenAndServe(listenAddress, http.DefaultServeMux))
}