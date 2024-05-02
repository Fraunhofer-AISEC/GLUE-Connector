package de.fraunhofer.aisec.glue.plugins

import de.fraunhofer.aisec.glue.ContainerPipeline
import de.fraunhofer.aisec.glue.PDP
import io.ktor.server.application.*
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

fun Application.configureDependencyInjection() {
    install(Koin) {
        modules(module {
            single { PDP() }
            single { mutableMapOf<String, ContainerPipeline>() }
        })
    }
}