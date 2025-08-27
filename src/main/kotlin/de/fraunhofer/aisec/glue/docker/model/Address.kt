package de.fraunhofer.aisec.glue.docker.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Address represents an IPv4 or IPv6 IP address.
 *
 * @param addr IP address.
 * @param prefixLen Mask length of the IP address.
 */
data class Address(
    // IP address.
    @param:JsonProperty("Addr")
    val addr: String? = null,
    // Mask length of the IP address.
    @param:JsonProperty("PrefixLen")
    val prefixLen: Int? = null,
)
