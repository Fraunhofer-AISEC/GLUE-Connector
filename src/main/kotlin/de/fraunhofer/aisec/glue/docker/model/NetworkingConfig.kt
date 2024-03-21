package de.fraunhofer.aisec.glue.docker.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * NetworkingConfig represents the container's networking configuration for each of its interfaces. It is used for the networking configs specified in the `docker create` and `docker network connect` commands.
 *
 * @param endpointsConfig A mapping of network name to endpoint configuration for that network.
 */
@Serializable
data class NetworkingConfig(
    // A mapping of network name to endpoint configuration for that network.
    @SerialName("EndpointsConfig")
    val endpointsConfig: Map<String, EndpointSettings>? = null,
)
