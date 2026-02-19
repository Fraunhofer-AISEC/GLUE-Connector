# GLUE Connector Example

This is a complete example of how to deploy and network the GLUE connector.

## Getting Started

Install the following dependencies:

**Archlinux**
```bash
pacman -S --needed docker docker-compose make openssl python python-jinja curl jq jo jdk-openjdk skopeo
```

**Debian/Ubuntu** (requires the [Docker Debian repository](https://docs.docker.com/engine/install/debian/)):
```bash
apt install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin make openssl python3 python3-jinja2 curl jq jo openjdk-25-jdk skopeo
```

**Nix** (Docker is required on the host system):
```bash
nix-shell -p jdk25 go python314 python314Packages.jinja2 jo skopeo curl jq jo
```

Then build the connector docker container by running `./gradlew jibDockerBuild` in the project root directory.

## Usage

The example uses a Makefile to manage different deployment scenarios.
To select an example scenario, use the `EXAMPLE` makefile variable.
Currently, the multi-stage example is available, with more scenarios to be added through additional policy and values files.

### Available Commands

- `make start` - Build and start the compose environment
- `make stop` - Stop the running compose environment
- `make deploy-pipeline` - Deploy the selected example policy to the compose environment

### Adding New Example Scenarios

To add a new example scenario, create a new policy and values file prefixed with the name of the scenario.
The values file should be in JSON format and contain a top-level `components` array listing the names for each host in the scenario.
The policy must contain an appropriate ODRL policy with container hashes to be substituted by envsubst at runtime.
