package de.fraunhofer.aisec.glue.controllers

import de.fraunhofer.aisec.glue.Utils
import de.fraunhofer.aisec.glue.models.negotiation.*
import de.fraunhofer.aisec.glue.negotiation.Negotiation
import de.fraunhofer.aisec.glue.negotiation.NegotiationManager
import io.ktor.http.*
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
    private fun stateErrorResponse(negotiation: Negotiation): ContractNegotiationError {
        // Since this function is only called from response callbacks, the negotiation's lock should be held up the call
        // stack.
        val state = negotiation.getStateMessageUnlocked()

        return ContractNegotiationError(
            providerPid = state.providerPid,
            consumerPid = state.consumerPid,
            code = "invalidState",
        )
    }

    private fun Route.registerRoutes20241() {
        post("/request") {
            val principal = call.principal<UserIdPrincipal>()!!
            val req = call.receive<ContractRequestMessage>()
            val originUrl = Utils.baseUrlForOrigin(call.request.origin, 1)

            val negotiation = negotiationManager.createFromRequest(req, originUrl, principal.name)
            if (negotiation == null) {
                call.respond(HttpStatusCode.NotFound)
                return@post
            }

            negotiation.processRequest(req, initial = true) { ok ->
                if (ok) {
                    call.respond(HttpStatusCode.Created, negotiation.getStateMessageUnlocked())
                } else {
                    call.respond(HttpStatusCode.BadRequest, stateErrorResponse(negotiation))
                }
            }
        }

        post("/offers") {
            val principal = call.principal<UserIdPrincipal>()!!
            val req = call.receive<ContractOfferMessage>()
            val originUrl = Utils.baseUrlForOrigin(call.request.origin, 1)

            val negotiation = negotiationManager.createFromOffer(req, originUrl, principal.name)
            if (negotiation == null) {
                call.respond(HttpStatusCode.NotFound)
                return@post
            }

            call.respond(HttpStatusCode.Created, negotiation.getStateMessage())
            negotiation.processOffer(req, initial = true) { ok ->
                if (ok) {
                    call.respond(HttpStatusCode.Created, negotiation.getStateMessageUnlocked())
                } else {
                    call.respond(HttpStatusCode.BadRequest, stateErrorResponse(negotiation))
                }
            }
        }

        route("/{id}") {
            get {
                val principal = call.principal<UserIdPrincipal>()!!
                val id = call.parameters["id"] ?: throw IllegalStateException("ID not found")

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
                val id = call.parameters["id"] ?: throw IllegalStateException("ID not found")
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

                negotiation.processRequest(req) { ok ->
                    if (ok) {
                        call.respond(HttpStatusCode.OK)
                    } else {
                        call.respond(HttpStatusCode.BadRequest, stateErrorResponse(negotiation))
                    }
                }
            }

            post("/offers") {
                val principal = call.principal<UserIdPrincipal>()!!
                val id = call.parameters["id"] ?: throw IllegalStateException("ID not found")
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
                negotiation.processOffer(req) { ok ->
                    if (ok) {
                        call.respond(HttpStatusCode.OK)
                    } else {
                        call.respond(HttpStatusCode.BadRequest, stateErrorResponse(negotiation))
                    }
                }
            }

            post("/events") {
                val principal = call.principal<UserIdPrincipal>()!!
                val id = call.parameters["id"] ?: throw IllegalStateException("ID not found")
                val req = call.receive<ContractNegotiationEventMessage>()

                // This endpoint is valid for both provider and consumer, so handle it in two different ways

                when (req.eventType) {
                    NegotiationEvent.ACCEPTED -> {
                        if (id != req.providerPid) {
                            call.respond(HttpStatusCode.BadRequest)
                            return@post
                        }

                        val negotiation = negotiationManager.getAsProvider(id, req.consumerPid, principal.name)
                        if (negotiation == null) {
                            call.respond(HttpStatusCode.NotFound)
                            return@post
                        }

                        negotiation.processAccept { ok ->
                            if (ok) {
                                call.respond(HttpStatusCode.OK)
                            } else {
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    stateErrorResponse(negotiation),
                                )
                            }
                        }
                    }
                    NegotiationEvent.FINALIZED -> {
                        if (id != req.consumerPid) {
                            call.respond(HttpStatusCode.BadRequest)
                            return@post
                        }

                        val negotiation = negotiationManager.getAsConsumer(id, req.providerPid, principal.name)
                        if (negotiation == null) {
                            call.respond(HttpStatusCode.NotFound)
                            return@post
                        }

                        negotiation.processFinalize { ok ->
                            if (ok) {
                                call.respond(HttpStatusCode.OK)
                            } else {
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    stateErrorResponse(negotiation),
                                )
                            }
                        }
                    }
                }
            }

            post("/agreement") {
                val principal = call.principal<UserIdPrincipal>()!!
                val id = call.parameters["id"] ?: throw IllegalStateException("ID not found")
                val req = call.receive<ContractAgreementMessage>()

                if (id != req.consumerPid) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                }

                val negotiation = negotiationManager.getAsConsumer(id, req.providerPid, principal.name)
                if (negotiation == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@post
                }

                negotiation.processAgreement(req) { ok ->
                    if (ok) {
                        call.respond(HttpStatusCode.OK)
                    } else {
                        call.respond(HttpStatusCode.BadRequest, stateErrorResponse(negotiation))
                    }
                }
            }

            post("/agreement/verification") {
                val principal = call.principal<UserIdPrincipal>()!!
                val id = call.parameters["id"] ?: throw IllegalStateException("ID not found")
                val req = call.receive<ContractAgreementVerificationMessage>()

                if (id != req.providerPid) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                }

                val negotiation = negotiationManager.getAsProvider(id, req.consumerPid, principal.name)
                if (negotiation == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@post
                }

                negotiation.processVerify { ok ->
                    if (ok) {
                        call.respond(HttpStatusCode.OK)
                    } else {
                        call.respond(HttpStatusCode.BadRequest, stateErrorResponse(negotiation))
                    }
                }
            }

            post("/termination") {
                val principal = call.principal<UserIdPrincipal>()!!
                val id = call.parameters["id"] ?: throw IllegalStateException("ID not found")
                val req = call.receive<ContractNegotiationTerminationMessage>()

                // This handler is a bit awkward since termination messages are the same for provider and consumer
                // We just try both, which is only safe since we make sure that all pids are only assigned once
                var negotiation = negotiationManager.getAsProvider(id, req.consumerPid, principal.name)

                // As a side effect, we also have to push our correctness checks back
                if (negotiation != null) {
                    if (id != req.providerPid) {
                        call.respond(HttpStatusCode.BadRequest)
                        return@post
                    }
                } else {
                    negotiation = negotiationManager.getAsConsumer(id, req.providerPid, principal.name)
                    if (negotiation != null) {
                        if (id != req.consumerPid) {
                            call.respond(HttpStatusCode.BadRequest)
                            return@post
                        }
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                        return@post
                    }
                }

                negotiation.processTermination(req) {
                    call.respond(HttpStatusCode.OK)
                }
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
