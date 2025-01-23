## API Documentation

The OpenAPI specification for the API is available [here](./doc/api.yaml).
Note that the api specification may not always be in sync with the implementation.

## HTTP-Server Example Using API

This repository also contains example container definitions that use the container pipeline
mechanism to spawn a simple HTTP echo server.
First, build the docker http echo server docker image:

```sh
pushd test-containers
PREFIX=container-pipeline docker compose build
popd
```

Then start two container pipeline connectors with the docker socket mounted
(Don't forget to stop the containers after or subsequent runs will fail).

```sh
# Replace with socat socket path from old example if needed
docker_socket=/var/run/docker.sock
for i in $(seq 2); do
  docker run \
    --rm \
    --name="connector-$i" \
    -v "$docker_socket":/var/run/docker.sock \
    -d \
    container-registry.gitlab.cc-asp.fraunhofer.de/ccit-glue/usage-control/container-pipeline:latest
  echo "Connector $i IP: $(docker inspect -f '{{ .NetworkSettings.Networks.bridge.IPAddress }}' "connector-$i")"
done
```

[This script](doc/api-example.sh) contains the logic to trigger the process.
The script will eventually be replaced with a web UI.

```sh
./doc/api-example.sh <Connector 1 IP>:8080 <Connector 2 IP>:8080 test-containers/example-policy.json
```

The script will print the port the pipeline is reachable on.
If everything worked, the following call should print `Test`.

```sh
curl -d Test http://localhost:<Pipeline Port>/echo
```

## Raw (old) HTTP-Server Example Usage

### Build the test container image

```sh
docker build -t container-pipeline/http-echo test-containers/http-echo
```
### Start socat to create socket proxy to dump requests and responses:

```sh
socat -v UNIX-LISTEN:/tmp/docker-socket.sock,fork UNIX-CONNECT:/var/run/docker.sock
```
This can be passed to the container manager by exporting the `DOCKER_SOCKET` environment variable.

### Run the connector:

```sh
./gradlew run
```

### Register the example policy:

```sh
./gradlew runPolicyTool --args="-c 127.0.0.1:8080 -p ./test-containers/example-policy.json"
```

### Create the demo pipeline:

```sh
curl -X POST -H "Content-Type: application/json" -v -u user:password -d '{"target": "http://dataowner.com/document:1234", "action": "http://www.w3.org/ns/odrl/2/use"}' http://localhost:8080/pipeline
```
The server will return the pipeline ID as well as the port the pipeline is listening on.

### Stop/Delete the demo pipeline

```sh
curl --request DELETE -u user:password http://localhost:8080/pipeline/<pipeline id>
```