package main

import (
	"io"
	"log"
	"net/http"
	"os"
)

type fileFlushWriter struct {
	file *os.File
}

func (w *fileFlushWriter) Write(buf []byte) (int, error) {
	n, err := w.file.Write(buf)
	if err != nil {
		return 0, err
	}

	err = w.file.Sync()
	if err != nil {
		return 0, err
	}

	return n, err
}

func echo(w http.ResponseWriter, r *http.Request) {
	log.Printf("Handling request from %s", r.RemoteAddr)

	reader := io.TeeReader(r.Body, &fileFlushWriter{os.Stdout})
	_, _ = io.Copy(w, reader)
}

func main() {
	log.Printf("Starting echo server on port 8080")

	http.HandleFunc("/echo", echo)
	log.Fatal(http.ListenAndServe("0.0.0.0:8080", http.DefaultServeMux))
}
