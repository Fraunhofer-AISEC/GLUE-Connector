package de.fraunhofer.aisec.glue.controllers.api

import de.fraunhofer.aisec.glue.controllers.Controller
import de.fraunhofer.aisec.glue.models.api.NegotiationCreationRequest
import de.fraunhofer.aisec.glue.models.api.NegotiationInfoResponse
import de.fraunhofer.aisec.glue.negotiation.NegotiationManager
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * This controller allows a user to initiate and manage contract negotiation processes.
 */
class NegotiationApiController(private val negotiationManager: NegotiationManager) : Controller {
    override fun Route.registerRoutes() {
        route("/api/negotiations") {
            // List all negotiations that are currently ongoing on the connector
            get {
                val negotiations =
                    negotiationManager.getNegotiations()
                        .map { negotiation ->
                            NegotiationInfoResponse(
                                id = negotiation.id,
                                role = negotiation.role,
                                peer = negotiation.peerIdentity ?: "unknown",
                                state = negotiation.state
                            )
                        }

                call.respond(negotiations)
            }

            post {
                val req = call.receive<NegotiationCreationRequest>()
                //  the url and protocol version is derived.
                //  For not, it is just the URL with some simple parsing.
                val peerUrl =
                    run {
                        val address = if (req.peer.contains("://")) req.peer else "http://${req.peer}"
                        val builder = URLBuilder(address)

                        if (builder.pathSegments.isEmpty()) {
                            builder.path("negotiations")
                        }

                        builder.buildString()
                    }

                val negotiationFuture = negotiationManager.startNegotiation(peerUrl, req.contractRequest)

                if (negotiationFuture.isCompleted) {
                    try {
                        negotiationFuture.await()
                        // This is supposed to fail
                        // If not, we have some really weired scheduling going on, but so be it
                    } catch (e: Exception) {
                        LOG.atDebug {
                            message = "Contract negotiation completed exceptionally"
                            cause = e
                            payload = mapOf("peerConnector" to req.peer, "contractRequest" to req.contractRequest.uid)
                        }
                        // Note: This could also be an internal server error, but we will assume
                        // that it's the user's fault
                        call.respond(HttpStatusCode.BadRequest, "$e")
                        return@post
                    }
                }

                call.respond(HttpStatusCode.Created)
            }

            delete {
                LOG.error { "Contract negotiation can currently not be cancelled" }
            }
        }
    }

    companion object {
        val LOG = KotlinLogging.logger {}
    }
}
