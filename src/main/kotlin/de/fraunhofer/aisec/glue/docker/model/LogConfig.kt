package de.fraunhofer.aisec.glue.docker.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * The logging configuration for this container
 *
 * @param type
 * @param config
 */
data class LogConfig(
    @param:JsonProperty("Type")
    val type: String? = null,
    @param:JsonProperty("Config")
    val config: Map<String, String>? = null,
)
