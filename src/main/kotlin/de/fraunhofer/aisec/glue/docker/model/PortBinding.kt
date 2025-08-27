package de.fraunhofer.aisec.glue.docker.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * PortBinding represents a binding between a host IP address and a host port.
 *
 * @param hostIp Host IP address that the container's port is mapped to.
 * @param hostPort Host port number that the container's port is mapped to.
 */
data class PortBinding(
    // Host IP address that the container's port is mapped to.
    @param:JsonProperty("HostIp")
    val hostIp: String? = null,
    // Host port number that the container's port is mapped to.
    @param:JsonProperty("HostPort")
    val hostPort: String? = null,
)
