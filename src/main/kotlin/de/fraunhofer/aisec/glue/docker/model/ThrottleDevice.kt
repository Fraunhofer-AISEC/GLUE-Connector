package de.fraunhofer.aisec.glue.docker.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * @param path Device path
 * @param rate Rate
 */
data class ThrottleDevice(
    // Device path
    @param:JsonProperty("Path")
    val path: String? = null,
    // Rate
    @param:JsonProperty("Rate")
    val rate: Long? = null,
)
