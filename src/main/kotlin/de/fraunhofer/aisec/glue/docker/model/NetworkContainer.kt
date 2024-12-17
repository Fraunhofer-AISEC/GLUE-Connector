package de.fraunhofer.aisec.glue.docker.model

import com.fasterxml.jackson.annotation.JsonProperty

data class NetworkContainer(
    @JsonProperty("Name")
    val name: String? = null,
    @JsonProperty("EndpointID")
    val endpointID: String? = null,
    @JsonProperty("MacAddress")
    val macAddress: String? = null,
    @JsonProperty("IPv4Address")
    val ipv4Address: String? = null,
    @JsonProperty("IPv6Address")
    val ipv6Address: String? = null,
)
