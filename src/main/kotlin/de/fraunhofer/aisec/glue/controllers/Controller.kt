package de.fraunhofer.aisec.glue.controllers

import io.ktor.server.routing.*

interface Controller {
    fun Route.registerRoutes()
}
