package de.fraunhofer.aisec.glue.pipeline

data class ContainerPort(
    /**
     * The port inside the container that is forwarded to the host.
     */
    val containerPort: UShort,
    /**
     * The port on the host where the container is reachable.
     */
    val hostPort: UShort,
    /**
     * The protocol for the port.
     * Either `tcp` or `udp`
     */
    val protocol: String,
)
