package de.fraunhofer.aisec.glue.docker.model

import com.fasterxml.jackson.annotation.JsonProperty

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
data class EndpointSettings(
    @param:JsonProperty("IPAMConfig")
    val ipAMConfig: EndpointIPAMConfig? = null,
    @param:JsonProperty("Links")
    val links: List<String>? = null,
    @param:JsonProperty("Aliases")
    val aliases: List<String>? = null,
    // Unique ID of the network.
    @param:JsonProperty("NetworkID")
    val networkID: String? = null,
    // Unique ID for the service endpoint in a Sandbox.
    @param:JsonProperty("EndpointID")
    val endpointID: String? = null,
    // Gateway address for this network.
    @param:JsonProperty("Gateway")
    val gateway: String? = null,
    // IPv4 address.
    @param:JsonProperty("IPAddress")
    val ipAddress: String? = null,
    // Mask length of the IPv4 address.
    @param:JsonProperty("IPPrefixLen")
    val ipPrefixLen: Int? = null,
    // IPv6 gateway address.
    @param:JsonProperty("IPv6Gateway")
    val ipv6Gateway: String? = null,
    // Global IPv6 address.
    @param:JsonProperty("GlobalIPv6Address")
    val globalIPv6Address: String? = null,
    // Mask length of the global IPv6 address.
    @param:JsonProperty("GlobalIPv6PrefixLen")
    val globalIPv6PrefixLen: Long? = null,
    // MAC address for the endpoint on this network.
    @param:JsonProperty("MacAddress")
    val macAddress: String? = null,
    // DriverOpts is a mapping of driver options and values. These options are passed directly to the driver and are driver specific.
    @param:JsonProperty("DriverOpts")
    val driverOpts: Map<String, String>? = null,
)
