package de.fraunhofer.aisec.glue.docker.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * PortBinding represents a binding between a host IP address and a host port.
 *
 * @param hostIp Host IP address that the container's port is mapped to.
 * @param hostPort Host port number that the container's port is mapped to.
 */
@Serializable
data class PortBinding(
    // Host IP address that the container's port is mapped to.
    @SerialName(value = "HostIp")
    val hostIp: String? = null,
    // Host port number that the container's port is mapped to.
    @SerialName(value = "HostPort")
    val hostPort: String? = null,
)
