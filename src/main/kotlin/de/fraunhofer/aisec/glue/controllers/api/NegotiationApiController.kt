package de.fraunhofer.aisec.glue.controllers.api

import de.fraunhofer.aisec.glue.Utils
import de.fraunhofer.aisec.glue.controllers.Controller
import de.fraunhofer.aisec.glue.models.api.NegotiationCreationRequest
import de.fraunhofer.aisec.glue.models.api.NegotiationInfoResponse
import de.fraunhofer.aisec.glue.negotiation.NegotiationManager
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * This controller allows a user to initiate and manage contract negotiation processes.
 */
class NegotiationApiController(private val negotiationManager: NegotiationManager) : Controller {
    override fun Route.registerRoutes() {
        // TODO: Authorization
        route("/api/negotiations") {
            // List all negotiations that are currently ongoing on the connector
            get {
                val negotiations = negotiationManager.getNegotiations()
                    .map { negotiation ->
                        val state = negotiation.getStateMessage()
                        NegotiationInfoResponse(
                            role = negotiation.role,
                            peer = negotiation.peerIdentity ?: "unknown",
                            providerPid = state.providerPid,
                            consumerPid = state.consumerPid,
                            state = state.state
                        )
                    }

                call.respond(negotiations)
            }

            post {
                val req = call.receive<NegotiationCreationRequest>()
                // TODO: Any reliable way to get the external URL of this connector
                //  By default, I would just use the URL the request was sent to (as I am doing now)
                //  That said, it is not inconceivable that the User API is eventually hosted on a different location to
                //  the one for different connectors.
                val originUrl = Utils.baseUrlForOrigin(call.request.origin, 2)
                val callbackUrl = URLBuilder(originUrl).appendPathSegments("negotiations").build()
                // TODO: The peer field from the request should probably be some sort of internal ID from which
                //  the url and protocol version is derived.
                //  For not, it is just the URL with some simple parsing.
                val peerUrl = run {
                    val address = if (req.peer.contains("://")) req.peer else "http://${req.peer}"
                    val builder = URLBuilder(address)

                    if (builder.pathSegments.isEmpty()) {
                        builder.path("negotiations")
                    }

                    builder.build()
                }

                val negotiationFuture = negotiationManager.startNegotiation(peerUrl, callbackUrl, req.contractRequest)

                if (negotiationFuture.isCompleted) {
                    try {
                        negotiationFuture.await()
                        // This is supposed to fail
                        // If not, we have some really weired scheduling going on, but so be it
                    } catch (e: Exception) {
                        // Note: This could also be an internal server error, but we will assume that its the user's
                        // fault
                        call.respond(HttpStatusCode.BadRequest, "$e")
                        return@post
                    }
                }

                call.respond(HttpStatusCode.Created)
            }

            delete {
                // TODO: Delete the negotiation object
                // TODO: How to identify? (providerPid, consumerPid)? some internal ID?
            }
        }
    }

}