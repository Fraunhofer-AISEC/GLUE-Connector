package de.fraunhofer.aisec.glue.docker.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Address represents an IPv4 or IPv6 IP address.
 *
 * @param addr IP address.
 * @param prefixLen Mask length of the IP address.
 */
@Serializable
data class Address(
    // IP address.
    @SerialName(value = "Addr")
    val addr: String? = null,
    // Mask length of the IP address.
    @SerialName(value = "PrefixLen")
    val prefixLen: Int? = null,
)
