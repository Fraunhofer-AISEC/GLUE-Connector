package de.fraunhofer.aisec.glue.docker.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param path
 * @param weight
 */
@Serializable
data class BlkioWeightDevice(
    @SerialName(value = "Path")
    val path: String? = null,
    @SerialName(value = "Weight")
    val weight: Int? = null,
)
