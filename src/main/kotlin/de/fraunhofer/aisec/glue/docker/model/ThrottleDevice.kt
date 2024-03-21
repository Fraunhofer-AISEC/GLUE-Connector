package de.fraunhofer.aisec.glue.docker.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param path Device path
 * @param rate Rate
 */
@Serializable
data class ThrottleDevice(
    // Device path
    @SerialName(value = "Path")
    val path: String? = null,
    // Rate
    @SerialName(value = "Rate")
    val rate: Long? = null,
)
