package de.fraunhofer.aisec.glue.docker.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * EndpointIPAMConfig represents an endpoint's IPAM configuration.
 *
 * @param ipv4Address
 * @param ipv6Address
 * @param linkLocalIPs
 */
@Serializable
data class EndpointIPAMConfig(
    @SerialName(value = "IPv4Address")
    val ipv4Address: String? = null,
    @SerialName(value = "IPv6Address")
    val ipv6Address: String? = null,
    @SerialName(value = "LinkLocalIPs")
    val linkLocalIPs: List<String>? = null,
)
