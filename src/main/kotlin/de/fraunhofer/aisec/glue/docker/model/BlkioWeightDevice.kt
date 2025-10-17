package de.fraunhofer.aisec.glue.docker.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * @param path
 * @param weight
 */
data class BlkioWeightDevice(
    @param:JsonProperty("Path")
    val path: String? = null,
    @param:JsonProperty("Weight")
    val weight: Int? = null,
)
