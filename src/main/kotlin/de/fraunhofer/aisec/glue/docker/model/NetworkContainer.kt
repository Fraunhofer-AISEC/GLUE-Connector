package de.fraunhofer.aisec.glue.docker.model

import com.fasterxml.jackson.annotation.JsonProperty

data class NetworkContainer(
    @param:JsonProperty("Name")
    val name: String? = null,
    @param:JsonProperty("EndpointID")
    val endpointID: String? = null,
    @param:JsonProperty("MacAddress")
    val macAddress: String? = null,
    @param:JsonProperty("IPv4Address")
    val ipv4Address: String? = null,
    @param:JsonProperty("IPv6Address")
    val ipv6Address: String? = null,
)
