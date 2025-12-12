package de.fraunhofer.aisec.glue.controllers

import io.ktor.server.routing.*

/**
 * Marker interface for Controllers managing HTTP endpoints on the ATLS-enforcing server.
 */
interface AtlsController {
    fun Route.registerRoutes()
}
