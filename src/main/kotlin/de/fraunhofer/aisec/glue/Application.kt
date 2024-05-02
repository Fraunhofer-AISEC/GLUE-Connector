package de.fraunhofer.aisec.glue

import de.fraunhofer.aisec.glue.plugins.configureAuthentication
import de.fraunhofer.aisec.glue.plugins.configureDependencyInjection
import de.fraunhofer.aisec.glue.plugins.configureRouting
import de.fraunhofer.aisec.glue.plugins.configureSerialization
import io.ktor.server.application.*

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    configureAuthentication()
    configureRouting()
    configureSerialization()
    configureDependencyInjection()
}
