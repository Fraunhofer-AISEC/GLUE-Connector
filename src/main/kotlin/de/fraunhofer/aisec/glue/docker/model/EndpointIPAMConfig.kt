package de.fraunhofer.aisec.glue.docker.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * EndpointIPAMConfig represents an endpoint's IPAM configuration.
 *
 * @param ipv4Address
 * @param ipv6Address
 * @param linkLocalIPs
 */
data class EndpointIPAMConfig(
    @JsonProperty("IPv4Address")
    val ipv4Address: String? = null,
    @JsonProperty("IPv6Address")
    val ipv6Address: String? = null,
    @JsonProperty("LinkLocalIPs")
    val linkLocalIPs: List<String>? = null,
)
