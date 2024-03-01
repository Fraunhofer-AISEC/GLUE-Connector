package main

import (
    "os"
    "net"
    "fmt"
    "log"
    "io"
)

func proxy(conn net.Conn, proxyAddress string) {
    defer conn.Close()

    peer, err := net.Dial("tcp", proxyAddress)
    if err != nil {
        log.Printf("Could not connect to proxy address: %s", err.Error())
        return
    }
    defer peer.Close()

    forward := func(from net.Conn, to net.Conn, errCh chan <-error) {
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

func main() {
    if len(os.Args) != 3 {
        fmt.Fprintf(os.Stderr, "Usage: %s <listen port> <proxy address>\n", os.Args[0])
        os.Exit(1)
    }

    listenPort := os.Args[1]
    proxyAddress := os.Args[2]

    listenAddress := fmt.Sprintf("0.0.0.0:%s", listenPort)
    log.Printf("Listening for connections on %s", listenAddress)

    listener, err := net.Listen("tcp", listenAddress)
    if err != nil {
        log.Fatalf("Failed to listen on port %s: %s", listenPort, err.Error())
    }

    for {
        conn, err := listener.Accept()
        log.Printf("Accepted connection from %s", conn.RemoteAddr().String())

        if err != nil {
            log.Printf("Failed to accept connection: %s", err.Error())
            continue
        }

        go proxy(conn, proxyAddress)
    }
}