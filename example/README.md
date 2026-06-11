# GLUE Connector Example

This directory contains examples for the deployment of the GLUE connector.
The examples consist of multiple instances of the connector and their supporting infrastructure deployed using docker compose.

Currently, only the multi-stage example is available, which consists of three connectors that exchange control- and
data-plane messages over attested TLS.

## Dependencies

Running the example requires docker compose, openssl, jq, jo, envsubst, curl, and skopeo.

### Debian-based systems

> [!NOTE]
> The installation command assumes that the [official docker repository](https://docs.docker.com/engine/install/debian/#install-using-the-repository) is configured.

```shell
apt install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin openssl jq jo gettext-base curl skopeo
```

### Arch Linux

```shell
pacman -S docker docker-buildx docker-compose openssl jq jo gettext curl skopeo
```

### Nix Shell

```shell
nix-shell -p docker-client docker-buildx docker-compose openssl jq jo gettext curl skopeo
```

## Usage

The main entrypoint for the example is the `example.sh` script.
It spins up all required connectors and supporting infrastructure with docker compose, initiates contract negotiation
and establishes data pipelines.

The script expects the name of the example to run as its first argument.
Additional arguments can be passed to alter the behaviour of the script as follows:

```
Usage: ./example.sh [Options] <example name>
Options:
  --insecure        Disable aTLS
  --regenerate-pki  Generate new certificates
  --refetch-images  Re-fetch all container images from the registry
  --initial         The name of the initial container (default: initiator)
  --intermediary    The name of the first contract recipient container (default: intermediary)
  --initial         The name of the final container (default: destination)
  --help            Show this message and exit
```
