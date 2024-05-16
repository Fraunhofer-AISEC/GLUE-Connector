package de.fraunhofer.aisec.glue.docker.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * NetworkSettings exposes the network settings in the API
 *
 * @param bridge Name of the network's bridge (for example, `docker0`).
 * @param sandboxID SandboxID uniquely represents a container's network stack.
 * @param hairpinMode Indicates if hairpin NAT should be enabled on the virtual interface.
 * @param linkLocalIPv6Address IPv6 unicast address using the link-local prefix.
 * @param linkLocalIPv6PrefixLen Prefix length of the IPv6 unicast address.
 * @param ports PortMap describes the mapping of container ports to host ports, using the container's port-number and protocol as key in the format `<port>/<protocol>`, for example, `80/udp`.  If a container's port is mapped for multiple protocols, separate entries are added to the mapping table.
 * @param sandboxKey SandboxKey identifies the sandbox
 * @param secondaryIPAddresses
 * @param secondaryIPv6Addresses
 * @param endpointID EndpointID uniquely represents a service endpoint in a Sandbox.  <p><br /></p>  > **Deprecated**: This field is only propagated when attached to the > default \"bridge\" network. Use the information from the \"bridge\" > network inside the `Networks` map instead, which contains the same > information. This field was deprecated in Docker 1.9 and is scheduled > to be removed in Docker 17.12.0
 * @param gateway Gateway address for the default \"bridge\" network.  <p><br /></p>  > **Deprecated**: This field is only propagated when attached to the > default \"bridge\" network. Use the information from the \"bridge\" > network inside the `Networks` map instead, which contains the same > information. This field was deprecated in Docker 1.9 and is scheduled > to be removed in Docker 17.12.0
 * @param globalIPv6Address Global IPv6 address for the default \"bridge\" network.  <p><br /></p>  > **Deprecated**: This field is only propagated when attached to the > default \"bridge\" network. Use the information from the \"bridge\" > network inside the `Networks` map instead, which contains the same > information. This field was deprecated in Docker 1.9 and is scheduled > to be removed in Docker 17.12.0
 * @param globalIPv6PrefixLen Mask length of the global IPv6 address.  <p><br /></p>  > **Deprecated**: This field is only propagated when attached to the > default \"bridge\" network. Use the information from the \"bridge\" > network inside the `Networks` map instead, which contains the same > information. This field was deprecated in Docker 1.9 and is scheduled > to be removed in Docker 17.12.0
 * @param ipAddress IPv4 address for the default \"bridge\" network.  <p><br /></p>  > **Deprecated**: This field is only propagated when attached to the > default \"bridge\" network. Use the information from the \"bridge\" > network inside the `Networks` map instead, which contains the same > information. This field was deprecated in Docker 1.9 and is scheduled > to be removed in Docker 17.12.0
 * @param ipPrefixLen Mask length of the IPv4 address.  <p><br /></p>  > **Deprecated**: This field is only propagated when attached to the > default \"bridge\" network. Use the information from the \"bridge\" > network inside the `Networks` map instead, which contains the same > information. This field was deprecated in Docker 1.9 and is scheduled > to be removed in Docker 17.12.0
 * @param ipv6Gateway IPv6 gateway address for this network.  <p><br /></p>  > **Deprecated**: This field is only propagated when attached to the > default \"bridge\" network. Use the information from the \"bridge\" > network inside the `Networks` map instead, which contains the same > information. This field was deprecated in Docker 1.9 and is scheduled > to be removed in Docker 17.12.0
 * @param macAddress MAC address for the container on the default \"bridge\" network.  <p><br /></p>  > **Deprecated**: This field is only propagated when attached to the > default \"bridge\" network. Use the information from the \"bridge\" > network inside the `Networks` map instead, which contains the same > information. This field was deprecated in Docker 1.9 and is scheduled > to be removed in Docker 17.12.0
 * @param networks Information about all networks that the container is connected to.
 */
@Serializable
data class NetworkSettings(
    // Name of the network's bridge (for example, `docker0`).
    @SerialName(value = "Bridge")
    val bridge: String? = null,
    // SandboxID uniquely represents a container's network stack.
    @SerialName(value = "SandboxID")
    val sandboxID: String? = null,
    // Indicates if hairpin NAT should be enabled on the virtual interface.
    @SerialName(value = "HairpinMode")
    val hairpinMode: Boolean? = null,
    // IPv6 unicast address using the link-local prefix.
    @SerialName(value = "LinkLocalIPv6Address")
    val linkLocalIPv6Address: String? = null,
    // Prefix length of the IPv6 unicast address.
    @SerialName(value = "LinkLocalIPv6PrefixLen")
    val linkLocalIPv6PrefixLen: Int? = null,
    // PortMap describes the mapping of container ports to host ports, using the container's port-number and protocol as key in the format `<port>/<protocol>`, for example, `80/udp`.  If a container's port is mapped for multiple protocols, separate entries are added to the mapping table.
    @SerialName(value = "Ports")
    val ports: Map<String, List<PortBinding>>? = null,
    // SandboxKey identifies the sandbox
    @SerialName(value = "SandboxKey")
    val sandboxKey: String? = null,
    //
    @SerialName(value = "SecondaryIPAddresses")
    val secondaryIPAddresses: List<Address>? = null,
    //
    @SerialName(value = "SecondaryIPv6Addresses")
    val secondaryIPv6Addresses: List<Address>? = null,
    // EndpointID uniquely represents a service endpoint in a Sandbox.  <p><br /></p>  > **Deprecated**: This field is only propagated when attached to the > default \"bridge\" network. Use the information from the \"bridge\" > network inside the `Networks` map instead, which contains the same > information. This field was deprecated in Docker 1.9 and is scheduled > to be removed in Docker 17.12.0
    @SerialName(value = "EndpointID")
    val endpointID: String? = null,
    // Gateway address for the default \"bridge\" network.  <p><br /></p>  > **Deprecated**: This field is only propagated when attached to the > default \"bridge\" network. Use the information from the \"bridge\" > network inside the `Networks` map instead, which contains the same > information. This field was deprecated in Docker 1.9 and is scheduled > to be removed in Docker 17.12.0
    @SerialName(value = "Gateway")
    val gateway: String? = null,
    // Global IPv6 address for the default \"bridge\" network.  <p><br /></p>  > **Deprecated**: This field is only propagated when attached to the > default \"bridge\" network. Use the information from the \"bridge\" > network inside the `Networks` map instead, which contains the same > information. This field was deprecated in Docker 1.9 and is scheduled > to be removed in Docker 17.12.0
    @SerialName(value = "GlobalIPv6Address")
    val globalIPv6Address: String? = null,
    // Mask length of the global IPv6 address.  <p><br /></p>  > **Deprecated**: This field is only propagated when attached to the > default \"bridge\" network. Use the information from the \"bridge\" > network inside the `Networks` map instead, which contains the same > information. This field was deprecated in Docker 1.9 and is scheduled > to be removed in Docker 17.12.0
    @SerialName(value = "GlobalIPv6PrefixLen")
    val globalIPv6PrefixLen: Int? = null,
    // IPv4 address for the default \"bridge\" network.  <p><br /></p>  > **Deprecated**: This field is only propagated when attached to the > default \"bridge\" network. Use the information from the \"bridge\" > network inside the `Networks` map instead, which contains the same > information. This field was deprecated in Docker 1.9 and is scheduled > to be removed in Docker 17.12.0
    @SerialName(value = "IPAddress")
    val ipAddress: String? = null,
    // Mask length of the IPv4 address.  <p><br /></p>  > **Deprecated**: This field is only propagated when attached to the > default \"bridge\" network. Use the information from the \"bridge\" > network inside the `Networks` map instead, which contains the same > information. This field was deprecated in Docker 1.9 and is scheduled > to be removed in Docker 17.12.0
    @SerialName(value = "IPPrefixLen")
    val ipPrefixLen: Int? = null,
    // IPv6 gateway address for this network.  <p><br /></p>  > **Deprecated**: This field is only propagated when attached to the > default \"bridge\" network. Use the information from the \"bridge\" > network inside the `Networks` map instead, which contains the same > information. This field was deprecated in Docker 1.9 and is scheduled > to be removed in Docker 17.12.0
    @SerialName(value = "IPv6Gateway")
    val ipv6Gateway: String? = null,
    // MAC address for the container on the default \"bridge\" network.  <p><br /></p>  > **Deprecated**: This field is only propagated when attached to the > default \"bridge\" network. Use the information from the \"bridge\" > network inside the `Networks` map instead, which contains the same > information. This field was deprecated in Docker 1.9 and is scheduled > to be removed in Docker 17.12.0
    @SerialName(value = "MacAddress")
    val macAddress: String? = null,
    // Information about all networks that the container is connected to.
    @Contextual @SerialName(value = "Networks")
    val networks: Map<String, EndpointSettings>? = null,
)
