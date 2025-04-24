package de.fraunhofer.aisec.glue.config

import io.ktor.server.config.*

/**
 * This class is used by [ConfigTest] to determine if names are resolved correctly.
 */
class TopLevelConfig(cfg: ApplicationConfig) : Config(cfg) {
    val testKey by required(stringProp())
}
