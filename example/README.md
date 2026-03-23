# GLUE Connector Example

This is a complete example of how to deploy and network the GLUE connector.

## Dependencies

TODO: Collect all dependencies with apt/pacman/etc. commands here.

## Setup

1. Generate the CMC PKI infrastructure by running `./generate-pki.sh`.
2. Build the connector docker image by running `./gradlew jibDockerBuiold` in the parent directory.
3. Run the desired example using docker compose with `docker compose -f <example>.compose.yml up`.

## Usage

TODO: Once the CMC proxy is merged upstream, add a special container that proxies requests.