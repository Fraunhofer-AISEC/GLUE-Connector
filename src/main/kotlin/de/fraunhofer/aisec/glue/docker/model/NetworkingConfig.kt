package de.fraunhofer.aisec.glue.docker.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * NetworkingConfig represents the container's networking configuration for each of its interfaces. It is used for the networking configs specified in the `docker create` and `docker network connect` commands.
 *
 * @param endpointsConfig A mapping of network name to endpoint configuration for that network.
 */
data class NetworkingConfig(
    // A mapping of network name to endpoint configuration for that network.
    @param:JsonProperty("EndpointsConfig")
    val endpointsConfig: Map<String, EndpointSettings>? = null,
)
