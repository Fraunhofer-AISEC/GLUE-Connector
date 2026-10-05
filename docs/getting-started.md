## Getting Started

> [!IMPORTANT]
> Irgendwo muss aber auch noch grundsätzlich stehen, wie man den Spaß konfiguriert und aufsetzt.
> Da kann wenns ist auch ich nochmal was schreiben.
> Die Frage wäre wo und in welchem Umfang.
>
> Rep: Es wird ja schon die eigentliche technische Anleitung verlinkt in den GLUE Examples. 
> Also wir müssen nicht alles doppelt schreiben. Diese Docu hier ist gedacht für einen Einstig der etwas höher-leveliger ist.

The GLUE repository contains a simple examples for demonstration. 
The corresponding code can be seen here: [GLUE Examples](https://gitlab.cc-asp.fraunhofer.de/ccit-glue/usage-control/container-pipeline/-/tree/main/example?ref_type=heads).
<!--- TODO Update the link -->

<!--- Taken from Code-README.md -->
This directory contains examples for the deployment of the Trusted Connector.
The examples use docker compose to orchestrate the required components as containers.

## Prerequisites

Running the example requires docker compose, openssl, yq, jo, envsubst, curl, and skopeo.
We assume that a docker daemon is already available and that you have access to it.
[Podman](https://podman.io/) may also work, but we do not officially support it.
In addition to the dependencies, the examples require that port `5000` is available on the host machine.

### Debian-based systems

The installation command assumes that the [official docker repository](https://docs.docker.com/engine/install/debian/#install-using-the-repository) is configured.

```shell
apt install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin openssl yq jo gettext-base curl skopeo
```

### Arch Linux

```shell
pacman -S docker docker-buildx docker-compose openssl yq jo gettext curl skopeo
```

### Nix Shell

```shell
nix-shell -p docker-client docker-buildx docker-compose openssl yq jo gettext curl skopeo
```

## Running Examples

To demonstrate the different capabilities of the Trusted Connector, we provide a set of examples.
Each example defines its own actors and policy that may deploy different pipeline configurations.
To start an example, run `./example.sh <example>` or run `./example.sh --help` for more information on its complete
functionality.
Possible options for `<example>` can be seen in the example folder. As chosen example `multi-stage` is used in the next explanation.

**Basic Multi-Stage Example:** The multi-stage example showcases a data processing chain consisting of three connectors
each running a separate processing pipeline.
The first connector (*initiator*) negotiates a contract with the second connector (*intermediary*), which then performs
a follow-up negotiation with the final connector (*destination*).
The follow-up policy is defined statically in the initial policy without dynamic parameters.
Run this example with `./example.sh multi-stage`.

## Example Internals

This section provides a short overview of the example infrastructure's internal workings.
It details how each connector is represented within the docker compose environment, what parts are included as
infrastructure components and how the attestation is performed in the example.

### Connector Deployment

All examples consist of one or more connector deployments and a set of supporting infrastructure.
We deploy each service in a separate docker container and link them together where necessary.
In a production deployment, each connector would consist of a single confidential VM with the required services running
directly on the system.
We use the docker compose `depends_on` and `healthcheck` features to make sure that services start in the correct order.
Related services communicate via a linked network namespace as well as a dedicated shared volume where we place shared
sockets and files.

|                                    ![](images/figures-connector-architecture.drawio.svg)                                    |
|:----------------------------------------------------------------------------------------------------------------:|
| Figure 1: Connector deployment with supporting services. Insecure deployments do not include components in grey. |

Figure 1 shows the services involved in each connector deployment as well as their interactions with each other as well
as with the external service containers.
Alongside the trusted connector itself, we deploy a separate docker daemon for each connector. The trusted connector
uses this docker daemon to manage individual pipeline containers.
To ensure that the examples work in any environment, we bundle all container images and load them into a local container
registry on startup.

The [Container Measurement Component (CMC)](https://github.com/Fraunhofer-AISEC/cmc) provides cross-platform attestation
capabilities via its API, which we use to attest the connectors.
We use a software TPM to serve as the attestation backend for the CMC. The CMC also manages the TLS certificate we use
in the connector.
It fetches these certificates via [Enrollment over Secure Transport (EST)](https://datatracker.ietf.org/doc/html/rfc7030)
from the EST server.
When using the `--insecure` flag, these containers are not used or created.









### What just happened?

The docker compose script above starts a sample container setup which allows the enforcement of usage control. 
The same data processing network can be launched on a remote system.


### More Information
More information about the provided examples, how they can be modified and further details of the configurable parameters can be found in the [GLUE Examples](https://gitlab.cc-asp.fraunhofer.de/ccit-glue/usage-control/container-pipeline/-/tree/main/example?ref_type=heads).

See the [Concept Overview](concept-overview.md) for more information on the concepts and design decisions underlying the GLUE Connector.