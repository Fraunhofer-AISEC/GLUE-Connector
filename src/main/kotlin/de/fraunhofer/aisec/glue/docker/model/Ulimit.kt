package de.fraunhofer.aisec.glue.docker.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 *  * @param name Name of ulimit
 *  * @param soft Soft limit
 *  * @param hard Hard limit
 */
@Serializable
data class Ulimit(
    @SerialName("Name")
    val name: String,
    @SerialName("Soft")
    val soft: Int,
    @SerialName("Hard")
    val hard: Int,
)
