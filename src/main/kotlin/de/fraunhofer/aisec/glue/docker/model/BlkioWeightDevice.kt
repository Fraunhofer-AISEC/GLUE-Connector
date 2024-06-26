package de.fraunhofer.aisec.glue.docker.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * @param path
 * @param weight
 */
data class BlkioWeightDevice(
    @JsonProperty("Path")
    val path: String? = null,
    @JsonProperty("Weight")
    val weight: Int? = null,
)
