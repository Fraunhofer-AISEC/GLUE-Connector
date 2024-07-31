package de.fraunhofer.aisec.glue.controllers

import de.fraunhofer.aisec.glue.models.negotiation.*
import de.fraunhofer.aisec.glue.negotiation.NegotiationManager
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Controller for the Data Space Protocol Contract Negotiation HTTP(S) protocol.
 * It currently implements v2024.1.
 * <https://docs.internationaldataspaces.org/ids-knowledgebase/v/dataspace-protocol/contract-negotiation/contract.negotiation.binding.https>
 */
class NegotiationController(private val negotiationManager: NegotiationManager) : Controller {
    // Since we may be behind a reverse proxy, make sure that we are reachable when we eventually send a message with a
    // callback URL
    private fun baseUrlForOrigin(
        origin: RequestConnectionPoint,
        backtrack: Int,
    ): Url {
        val segments = origin.uri.split('/').dropLast(backtrack)
        val protocol = URLProtocol.createOrDefault(origin.scheme)
        val builder =
            URLBuilder(
                protocol = protocol,
                host = origin.serverHost,
                pathSegments = segments,
            )
        if (protocol.defaultPort != origin.serverPort) {
            builder.port = origin.serverPort
        }
        return builder.build()
    }

    private fun Route.registerRoutes20241() {
        post("/request") {
            val principal = call.principal<UserIdPrincipal>()!!
            val req = call.receive<ContractRequestMessage>()
            val originUrl = baseUrlForOrigin(call.request.origin, 1)

            val negotiation = negotiationManager.createFromRequest(req, originUrl, principal.name)
            if (negotiation == null) {
                call.respond(HttpStatusCode.NotFound)
                return@post
            }

            call.respond(HttpStatusCode.Created, negotiation.getStateMessage())

            // We call this after returning from the HTTP call in order to (hopefully) send the offer after the response
            // of our negotiation.
            // We unfortunately don't control the network behaviour, so there is no guarantee that the messages arrive
            // in order.
            negotiation.processRequest(req, initial = true)
        }

        post("/offers") {
            val principal = call.principal<UserIdPrincipal>()!!
            val req = call.receive<ContractOfferMessage>()
            val originUrl = baseUrlForOrigin(call.request.origin, 1)

            val negotiation = negotiationManager.createFromOffer(req, originUrl, principal.name)
            if (negotiation == null) {
                call.respond(HttpStatusCode.NotFound)
                return@post
            }

            call.respond(HttpStatusCode.Created, negotiation.getStateMessage())
            negotiation.processOffer(req, initial = true)
        }

        route("/{id}") {
            get {
                val principal = call.principal<UserIdPrincipal>()!!
                val id = call.parameters["id"]!!

                // This endpoint is only defined on the provider side, so only lookup negotiations where we are the
                // provider
                val negotiation = negotiationManager.getAsProvider(id, null, principal.name)
                if (negotiation == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                }

                call.respond(negotiation.getStateMessage())
            }

            post("/request") {
                val principal = call.principal<UserIdPrincipal>()!!
                val id = call.parameters["id"]!!
                val req = call.receive<ContractRequestMessage>()

                if (id != req.providerPid) {
                    // If this happens, we don't have a clear indication which id to use -> just reject it.
                    // This also ensures that no new request are posted to this endpoint.
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                }

                val negotiation = negotiationManager.getAsProvider(id, req.consumerPid, principal.name)
                if (negotiation == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@post
                }

                // Again, respond before processing to get the ordering right
                call.respond(HttpStatusCode.OK)
                negotiation.processRequest(req)
            }

            post("/offers") {
                val principal = call.principal<UserIdPrincipal>()!!
                val id = call.parameters["id"]!!
                val req = call.receive<ContractOfferMessage>()

                if (id != req.consumerPid) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                }

                val negotiation = negotiationManager.getAsConsumer(id, req.providerPid, principal.name)
                if (negotiation == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@post
                }

                call.respond(HttpStatusCode.OK)
                negotiation.processOffer(req)
            }

            post("/events") {
                val principal = call.principal<UserIdPrincipal>()
                val id = call.parameters["id"]!!
                val req = call.receive<ContractNegotiationEventMessage>()

                // TODO: Process peer event
            }

            post("/agreement") {
                val principal = call.principal<UserIdPrincipal>()
                val id = call.parameters["id"]!!
                val req = call.receive<ContractAgreementMessage>()

                // TODO: Process agreement
            }

            post("/agreement/verification") {
                val principal = call.principal<UserIdPrincipal>()
                val id = call.parameters["id"]!!
                val req = call.receive<ContractAgreementVerificationMessage>()

                // TODO: Process verification message
            }

            post("/termination") {
                val principal = call.principal<UserIdPrincipal>()
                val id = call.parameters["id"]!!
                val req = call.receive<ContractNegotiationTerminationMessage>()

                // TODO: Process termination message
            }
        }
    }

    override fun Route.registerRoutes() {
        // TODO: Actually configure authentication
        authenticate {
            route("/2024/1/negotiations") {
                registerRoutes20241()
            }
            route("/negotiations") {
                registerRoutes20241()
            }
        }
    }
}
