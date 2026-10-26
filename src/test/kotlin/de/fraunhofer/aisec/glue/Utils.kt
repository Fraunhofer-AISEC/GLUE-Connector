package de.fraunhofer.aisec.glue

import de.fraunhofer.aisec.glue.config.ConfigManager
import io.ktor.server.config.MapApplicationConfig

fun emptyConfigManager(): ConfigManager {
    return ConfigManager(MapApplicationConfig())
}