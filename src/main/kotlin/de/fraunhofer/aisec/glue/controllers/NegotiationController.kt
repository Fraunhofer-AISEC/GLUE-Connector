package de.fraunhofer.aisec.glue.controllers

import io.ktor.server.routing.*

interface NegotiationController : AtlsController {
    override fun Route.registerRoutes() {}
}
