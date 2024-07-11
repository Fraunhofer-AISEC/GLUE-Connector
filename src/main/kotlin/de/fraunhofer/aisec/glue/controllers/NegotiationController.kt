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
    // TODO: Inject this so we can configure one client globally
    private val httpClient = HttpClient()

    private fun Route.registerRoutes20241() {
        post("/request") {
            val principal = call.principal<UserIdPrincipal>()
            val req = call.receive<ContractRequestMessage>()

            println(req)

            // TODO: Store and process contract offer
            // TODO: Allocate provider PID and return it to the caller

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

            // TODO: Process contract offer and allocate consumer PID

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

                // TODO: Look up negotiation state by provider PID (not found -> return 404)
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

                // TODO: Store the offer under the selected provider PID
            }

            post("/offers") {
                val principal = call.principal<UserIdPrincipal>()
                val id = call.parameters["id"]!!
                val req = call.receive<ContractOfferMessage>()

                // TODO: Process offer
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
