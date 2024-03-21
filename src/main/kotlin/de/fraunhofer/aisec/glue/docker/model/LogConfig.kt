package de.fraunhofer.aisec.glue.docker.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The logging configuration for this container
 *
 * @param type
 * @param config
 */
@Serializable
data class LogConfig(
    @SerialName(value = "Type")
    val type: String? = null,
    @SerialName(value = "Config")
    val config: Map<String, String>? = null,
)
