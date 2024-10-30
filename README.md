## API Documentation

The OpenAPI specification for the API is available [here](./doc/api.yaml).
Note that the api specification may not always be in sync with the implementation.

## HTTP-Server Example Usage

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