package de.fraunhofer.aisec.glue.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import de.fraunhofer.aisec.glue.config.ConnectorConfig
import de.fraunhofer.aisec.glue.models.negotiation.*
import de.fraunhofer.aisec.glue.negotiation.Negotiation
import de.fraunhofer.aisec.glue.negotiation.NegotiationManager
import de.fraunhofer.aisec.glue.negotiation.builtin.BuiltinNegotiationManager
import de.fraunhofer.aisec.glue.negotiation.edc.EdcQuirks.makeIncomingEdcPatcher
import de.fraunhofer.aisec.glue.negotiation.edc.EdcQuirks.makeOutgoingEdcPatcher
import de.fraunhofer.aisec.glue.plugins.ServerEdcCompatTransformation
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.getOrFail

/**
 * Controller for the Data Space Protocol Contract Negotiation HTTP(S) protocol.
 * It currently implements v2024.1.
 * <https://docs.internationaldataspaces.org/ids-knowledgebase/v/dataspace-protocol/contract-negotiation/contract.negotiation.binding.https>
 */
open class BuiltinNegotiationController(
    negotiationManager: NegotiationManager,
    private val connectorConfig: ConnectorConfig,
    private val objectMapper: ObjectMapper? = null,
) : NegotiationController {
    private val negotiationManager: BuiltinNegotiationManager =
        negotiationManager as? BuiltinNegotiationManager
            ?: throw IllegalArgumentException("NegotiationManager must be of type BuiltinNegotiationManager")

    private fun Route.registerRoutes20241() {
        // Install EDC compatibility plugin if required
        if (connectorConfig.edcMode == ConnectorConfig.EdcMode.COMPAT) {
            if (objectMapper == null) {
                throw IllegalArgumentException("ObjectMapper must be set when using EDC compatibility mode!")
            }
            install(ServerEdcCompatTransformation) {
                transformRequest = makeIncomingEdcPatcher(objectMapper, "server")
                transformResponse = makeOutgoingEdcPatcher(objectMapper, "server")
            }
        }

        post("/request") {
            val principal = call.principal<UserIdPrincipal>()
            val req = call.receive<ContractRequestMessage>()

            val peerUrl =
                try {
                    Url(req.callbackAddress)
                } catch (_: URLParserException) {
                    throw BadRequestException("Invalid callback address")
                }
            val negotiation =
                negotiationManager.createNegotiation(
                    Negotiation.Role.PROVIDER,
                    principal?.name,
                    req.consumerPid,
                    peerUrl,
                )

            when (val resp = negotiation.submitMessage(req)) {
                is ContractNegotiationError -> call.respond(HttpStatusCode.BadRequest, resp)
                // TODO: For some reason (prob. nullability), I cannot put an else branch here?
                is DspMessage -> call.respond(HttpStatusCode.Created, resp)
            }
        }

        post("/offers") {
            val principal = call.principal<UserIdPrincipal>()!!
            val req = call.receive<ContractOfferMessage>()

            val peerUrl =
                try {
                    Url(req.callbackAddress)
                } catch (_: URLParserException) {
                    throw BadRequestException("Invalid callback address")
                }
            val negotiation =
                negotiationManager.createNegotiation(
                    Negotiation.Role.CONSUMER,
                    principal.name,
                    req.providerPid,
                    peerUrl,
                )

            when (val resp = negotiation.submitMessage(req)) {
                is ContractNegotiationError -> call.respond(HttpStatusCode.BadRequest, resp)
                is DspMessage -> call.respond(HttpStatusCode.Created, resp)
            }
        }

        route("/{id}") {
            get {
                val principal = call.principal<UserIdPrincipal>()!!
                val id = call.parameters.getOrFail<String>("id")

                val negotiation = negotiationManager.getNegotiation(id, null, principal.name) ?: throw NotFoundException()

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

                val negotiation =
                    negotiationManager.getNegotiation(
                        id,
                        Negotiation.Role.PROVIDER,
                        principal.name,
                    ) ?: throw NotFoundException()

                when (val resp = negotiation.submitMessage(req)) {
                    is ContractNegotiationError -> call.respond(HttpStatusCode.BadRequest, resp)
                    else -> call.respond(HttpStatusCode.OK)
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

                val negotiation =
                    negotiationManager.getNegotiation(
                        id,
                        Negotiation.Role.CONSUMER,
                        principal.name,
                    ) ?: throw NotFoundException()

                when (val resp = negotiation.submitMessage(req)) {
                    is ContractNegotiationError -> call.respond(HttpStatusCode.BadRequest, resp)
                    else -> call.respond(HttpStatusCode.OK)
                }
            }

            post("/events") {
                val principal = call.principal<UserIdPrincipal>()
                val id = call.parameters["id"] ?: throw IllegalStateException("ID not found")
                val req = call.receive<ContractNegotiationEventMessage>()

                // This endpoint is valid for both provider and consumer, so handle it in two different ways
                when (req.eventType) {
                    NegotiationEvent.ACCEPTED -> {
                        if (id != req.providerPid) {
                            call.respond(HttpStatusCode.BadRequest)
                            return@post
                        }

                        val negotiation =
                            negotiationManager.getNegotiation(
                                id,
                                Negotiation.Role.PROVIDER,
                                principal?.name,
                            ) ?: throw NotFoundException()

                        when (val resp = negotiation.submitMessage(req)) {
                            null -> call.respond(HttpStatusCode.OK)
                            is ContractNegotiationError -> call.respond(HttpStatusCode.BadRequest, resp)
                        }
                    }
                    NegotiationEvent.FINALIZED -> {
                        if (id != req.consumerPid) {
                            call.respond(HttpStatusCode.BadRequest)
                            return@post
                        }

                        val negotiation =
                            negotiationManager.getNegotiation(
                                id,
                                Negotiation.Role.CONSUMER,
                                principal?.name,
                            ) ?: throw NotFoundException()

                        when (val resp = negotiation.submitMessage(req)) {
                            is ContractNegotiationError -> call.respond(HttpStatusCode.BadRequest, resp)
                            else -> call.respond(HttpStatusCode.OK)
                        }
                    }
                }
            }

            post("/agreement") {
                val principal = call.principal<UserIdPrincipal>()
                val id = call.parameters["id"] ?: throw IllegalStateException("ID not found")
                val req = call.receive<ContractAgreementMessage>()

                if (id != req.consumerPid) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                }

                val negotiation =
                    negotiationManager.getNegotiation(
                        id,
                        Negotiation.Role.CONSUMER,
                        principal?.name,
                    ) ?: throw NotFoundException()
                when (val resp = negotiation.submitMessage(req)) {
                    is ContractNegotiationError -> call.respond(HttpStatusCode.BadRequest, resp)
                    else -> call.respond(HttpStatusCode.OK)
                }
            }

            post("/agreement/verification") {
                val principal = call.principal<UserIdPrincipal>()
                val id = call.parameters["id"] ?: throw IllegalStateException("ID not found")
                val req = call.receive<ContractAgreementVerificationMessage>()

                if (id != req.providerPid) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                }

                val negotiation =
                    negotiationManager.getNegotiation(
                        id,
                        Negotiation.Role.PROVIDER,
                        principal?.name,
                    ) ?: throw NotFoundException()
                when (val resp = negotiation.submitMessage(req)) {
                    is ContractNegotiationError -> call.respond(HttpStatusCode.BadRequest, resp)
                    else -> call.respond(HttpStatusCode.OK)
                }
            }

            post("/termination") {
                val principal = call.principal<UserIdPrincipal>()
                val id = call.parameters["id"] ?: throw IllegalStateException("ID not found")
                val req = call.receive<ContractNegotiationTerminationMessage>()

                val negotiation = negotiationManager.getNegotiation(id, null, principal?.name) ?: throw NotFoundException()

                // One of the IDs has to match depending on the role of the found negotiation.
                if (
                    (id != req.providerPid && negotiation.role == Negotiation.Role.CONSUMER) ||
                    (id != req.consumerPid && negotiation.role == Negotiation.Role.PROVIDER)
                ) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                }

                negotiation.submitMessage(req)
                call.respond(HttpStatusCode.OK)
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

    companion object {
        private val LOG = KotlinLogging.logger {}
    }
}
