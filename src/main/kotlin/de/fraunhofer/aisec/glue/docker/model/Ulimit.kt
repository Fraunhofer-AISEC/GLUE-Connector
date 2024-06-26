package de.fraunhofer.aisec.glue.docker.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *  * @param name Name of ulimit
 *  * @param soft Soft limit
 *  * @param hard Hard limit
 */
data class Ulimit(
    @JsonProperty("Name")
    val name: String,
    @JsonProperty("Soft")
    val soft: Int,
    @JsonProperty("Hard")
    val hard: Int,
)
