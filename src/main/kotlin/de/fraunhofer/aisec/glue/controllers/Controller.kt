package de.fraunhofer.aisec.glue.controllers

import io.ktor.server.routing.*

/**
 * Marker interface for Controllers managing HTTP endpoints on the standard server, i.e.,
 * the ktor-managed server for internal communication/APIs.
 */
interface Controller {
    fun Route.registerRoutes()
}
