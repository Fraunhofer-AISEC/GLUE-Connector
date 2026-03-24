# GLUE Connector Example

This directory contains examples for the deployment of the GLUE connector.
The examples consist of multiple instances of the connector and their supporting infrastructure deployed using docker compose.

Currently, only the multi-stage example is available, which consists of three connectors that exchange control- and
data-plane messages over attested TLS.

## Dependencies

In addition to the requirements for building the GLUE connector itself, the examples require docker compose, openssl, jq, jo, envsubst, and curl.

### Debian-based systems

> [!NOTE]
> The installation command assumes that the [official docker repository](https://docs.docker.com/engine/install/debian/#install-using-the-repository) is configured.

```shell
apt install openjdk-25-jdk docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin openssl jq jo gettext-base curl
```

### Arch Linux

```shell
pacman -S jdk25-openjdk docker docker-buildx docker-compose openssl jq jo gettext curl
```

### Nix Shell

```shell
nix-shell -p jdk25 docker docker-buildx docker-compose openssl jq jo gettext curl
```

## Setup

1. Generate the CMC PKI infrastructure by running `./generate-pki.sh`.
2. Build the connector docker image by running `./gradlew jibDockerBuild` in the parent directory.
3. Run the desired example using docker compose with `docker compose -f <example>.compose.yml up`.

## Usage (multi-stage example)

Initiate a contract negotiation between the first and second connector and spawn the first container pipeline using `./run-example.sh initial intermediary multi-stage-policy.json`.
When the negotiation is finished, the script will print the IP address and port of the first, locally deployed pipeline.
POSTing an HTTP request to the provided addresses `/echo` endpoint will trigger the creation of follow-up pipelines on the subsequent connectors, which will eventually echo the input data.
