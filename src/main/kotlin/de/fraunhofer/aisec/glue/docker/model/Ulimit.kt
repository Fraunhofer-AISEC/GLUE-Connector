package de.fraunhofer.aisec.glue.docker.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *  * @param name Name of ulimit
 *  * @param soft Soft limit
 *  * @param hard Hard limit
 */
data class Ulimit(
    @param:JsonProperty("Name")
    val name: String,
    @param:JsonProperty("Soft")
    val soft: Int,
    @param:JsonProperty("Hard")
    val hard: Int,
)
