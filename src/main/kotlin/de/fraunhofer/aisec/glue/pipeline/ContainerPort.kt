package de.fraunhofer.aisec.glue.pipeline

data class ContainerPort(
    /**
     * The port inside the container that is forwarded to the host.
     */
    val containerPort: UShort,
    /**
     * The host:port pair where the container port is reachable on.
     */
    val hostAddress: String,
    /**
     * The protocol for the port.
     * Either `tcp` or `udp`.
     * Other protocols like `sctp` are currently not supported.
     */
    val protocol: String,
)
