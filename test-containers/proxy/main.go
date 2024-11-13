package main

import (
    "os"
    "net"
    "fmt"
    "log"
    "io"
    "encoding/json"
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

type config struct {
    // Controller address
    Controller string `json:"controller"`
    // The address of the next controller
    NextContainer string `json:"nextContainer"`
}

func main() {
    listenPort := "8080"

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

    proxyAddress := fmt.Sprintf("%s:8080", config.NextContainer)

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