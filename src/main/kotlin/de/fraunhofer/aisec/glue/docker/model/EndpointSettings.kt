package de.fraunhofer.aisec.glue.docker.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Configuration for a network endpoint.
 *
 * @param ipAMConfig
 * @param links
 * @param aliases
 * @param networkID Unique ID of the network.
 * @param endpointID Unique ID for the service endpoint in a Sandbox.
 * @param gateway Gateway address for this network.
 * @param ipAddress IPv4 address.
 * @param ipPrefixLen Mask length of the IPv4 address.
 * @param ipv6Gateway IPv6 gateway address.
 * @param globalIPv6Address Global IPv6 address.
 * @param globalIPv6PrefixLen Mask length of the global IPv6 address.
 * @param macAddress MAC address for the endpoint on this network.
 * @param driverOpts DriverOpts is a mapping of driver options and values. These options are passed directly to the driver and are driver specific.
 */
@Serializable
data class EndpointSettings(
    @SerialName(value = "IPAMConfig")
    val ipAMConfig: EndpointIPAMConfig? = null,
    @SerialName(value = "Links")
    val links: List<String>? = null,
    @SerialName(value = "Aliases")
    val aliases: List<String>? = null,
    // Unique ID of the network.
    @SerialName(value = "NetworkID")
    val networkID: String? = null,
    // Unique ID for the service endpoint in a Sandbox.
    @SerialName(value = "EndpointID")
    val endpointID: String? = null,
    // Gateway address for this network.
    @SerialName(value = "Gateway")
    val gateway: String? = null,
    // IPv4 address.
    @SerialName(value = "IPAddress")
    val ipAddress: String? = null,
    // Mask length of the IPv4 address.
    @SerialName(value = "IPPrefixLen")
    val ipPrefixLen: Int? = null,
    // IPv6 gateway address.
    @SerialName(value = "IPv6Gateway")
    val ipv6Gateway: String? = null,
    // Global IPv6 address.
    @SerialName(value = "GlobalIPv6Address")
    val globalIPv6Address: String? = null,
    // Mask length of the global IPv6 address.
    @SerialName(value = "GlobalIPv6PrefixLen")
    val globalIPv6PrefixLen: Long? = null,
    // MAC address for the endpoint on this network.
    @SerialName(value = "MacAddress")
    val macAddress: String? = null,
    // DriverOpts is a mapping of driver options and values. These options are passed directly to the driver and are driver specific.
    @SerialName(value = "DriverOpts")
    val driverOpts: Map<String, String>? = null,
)
