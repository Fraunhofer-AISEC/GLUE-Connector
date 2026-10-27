# GLUE Connector

> [!IMPORTANT]
> The GLUE connector is currently not ready for production use.
> Please note the [security considerations section](#security-considerations) for current security-related issues.
> We are working on resolving these issues in the near future.

The Generalized Lightweight Usage Enforcement (GLUE) framework enables flexible and secure data processing in
Trusted Execution Environments (TEEs).
The framework specifies data processing pipelines as a chain of containers that each perform one processing step.
A data owner defines their pipeline using a custom ODRL profile described [here](docs/concept-details.md#policies).
The data owner may also restrict how derived data is used by including a templated follow-up policy that is dynamically
instantiated by GLUE when data leaves the pipeline.

The GLUE connector is the central orchestrator for the GLUE framework.
It drives policy negotiation using the [Data Space Protocol](https://eclipse-dataspace-protocol-base.github.io/DataspaceProtocol/2025-1-err2/)
and manages data processing containers.
It also provides secure and attested connections to and between processing pipelines running on different hosts based on
the [CMC](https://github.com/Fraunhofer-AISEC/cmc).
For more information about the functionality and design decisions of the GLUE connector, please visit
[our documentation](docs/introduction.md).

## Getting Started

The GLUE connector is written in Kotlin and built using Gradle.
To build the connector locally, one of the following commands may be used.

```shell
# Compile only
./gradlew classes
# Build a self-contained jar file
./gradlew shadowJar
# Build a container image and load it into the local docker daemon
./gradlew jibDockerBuild
```

To test and run the GLUE connector, the [CMC](https://github.com/Fraunhofer-AISEC/cmc) and [Docker](https://docker.com)
are required.
Once both components are installed, run tests with `./gradlew test`.
For a full usage example, see the [example directory](example/).

## Security Considerations

The GLUE Connector is currently provided as a prototype implementation.
As such, a number of expected features to deploy an internet-facing service are currently still in  development, which
includes security features.
As such, deployments should currently be confined to internal networks or require a properly configured firewall.

The current version of the GLUE connector does not implement authentication or authorization for any HTTP API.
This includes both the user-facing API on port 8080 and the inter-connector API on port 8444.
For the inter-connector API we require attested TLS connections that use the CA certificates provided by the CMC.
As such, exposing port 8444 to external networks poses a smaller risk, provided the CMC is properly configured.
The same applies to the input and output ports of each processing pipeline, which uses the same attested TLS protocol
with the CMC's certificates.

Due to the use of Linked Data formats (Json-LD), some requests (including those involved in policy negotiation) can
trigger requests to third-party servers.
We currently don't implement white-listing to prevent accesses to local networks etc.
For the time being, only trusted parties should be allowed to access the negotiation interface.
