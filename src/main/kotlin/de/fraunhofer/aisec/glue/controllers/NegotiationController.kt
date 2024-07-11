package de.fraunhofer.aisec.glue.controllers

import de.fraunhofer.aisec.glue.models.negotiation.*
import io.ktor.client.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Controller for the Data Space Protocol Contract Negotiation HTTP(S) protocol.
 * It currently implements v2024.1.
 * <https://docs.internationaldataspaces.org/ids-knowledgebase/v/dataspace-protocol/contract-negotiation/contract.negotiation.binding.https>
 */
class NegotiationController : Controller {
    private val httpClient = HttpClient()

    private fun Route.registerRoutes20241() {
        post("/request") {
            val principal = call.principal<UserIdPrincipal>()
            val req = call.receive<ContractRequestMessage>()

            println(req)


            call.respond(
                HttpStatusCode.Created,
                ContractNegotiation(
                    providerPid = "Test",
                    consumerPid = req.consumerPid,
                    state = NegotiationState.REQUESTED,
                ),
            )
        }

        post("/offers") {
            val principal = call.principal<UserIdPrincipal>()
            val req = call.receive<ContractOfferMessage>()


            call.respond(
                HttpStatusCode.Created,
                ContractNegotiation(
                    providerPid = req.providerPid,
                    consumerPid = "Test",
                    state = NegotiationState.OFFERED,
                ),
            )
        }

        route("/{id}") {
            get {
                val principal = call.principal<UserIdPrincipal>()
                val id = call.parameters["id"]!!

                print("Get negotiation")

                call.respond(
                    ContractNegotiation(
                        providerPid = id,
                        consumerPid = "Test",
                        state = NegotiationState.REQUESTED,
                    ),
                )
            }

            post("/request") {
                val principal = call.principal<UserIdPrincipal>()
                val id = call.parameters["id"]!!
                val req = call.receive<ContractRequestMessage>()

            }

            post("/offers") {
                val principal = call.principal<UserIdPrincipal>()
                val id = call.parameters["id"]!!
                val req = call.receive<ContractOfferMessage>()

            }

            post("/events") {
                val principal = call.principal<UserIdPrincipal>()
                val id = call.parameters["id"]!!
                val req = call.receive<ContractNegotiationEventMessage>()

            }

            post("/agreement") {
                val principal = call.principal<UserIdPrincipal>()
                val id = call.parameters["id"]!!
                val req = call.receive<ContractAgreementMessage>()

            }

            post("/agreement/verification") {
                val principal = call.principal<UserIdPrincipal>()
                val id = call.parameters["id"]!!
                val req = call.receive<ContractAgreementVerificationMessage>()

            }

            post("/termination") {
                val principal = call.principal<UserIdPrincipal>()
                val id = call.parameters["id"]!!
                val req = call.receive<ContractNegotiationTerminationMessage>()

            }
        }
    }

    override fun Route.registerRoutes() {
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
