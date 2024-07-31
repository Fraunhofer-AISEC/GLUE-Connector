package de.fraunhofer.aisec.glue.negotiation

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.fraunhofer.aisec.glue.models.negotiation.*
import fhg.glue.odrl.OdrlPolicy
import fhg.glue.odrl.OdrlPolicyParser
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class Negotiation(
    var state: NegotiationState,
    var peerIdentity: String?,
    // TODO: (How) should we identify and authenticate the peer if we are initiating the negotiation (apart from TLS)
    val providerPid: String,
    val consumerPid: String,
    val ownEndpoint: Url,
    val peerEndpoint: Url,
    val httpClient: HttpClient,
    val negotiator: Negotiator,
) {
    val completed = CompletableDeferred<OdrlPolicy>(null)
    private val mutex = Mutex()

    private fun ensureState(vararg allowed: NegotiationState) {
        if (!allowed.contains(state)) {
            throw RuntimeException("Invalid State")
        }
    }

    private suspend inline fun <reified T> sendMessage(
        endpoint: String,
        message: T,
    ): HttpResponse {
        val url =
            URLBuilder(peerEndpoint).apply {
                path(endpoint)
            }.build()

        return httpClient.post(url) {
            setBody(message)
        }
    }

    suspend fun processRequest(
        req: ContractRequestMessage,
        initial: Boolean = false,
    ) = mutex.withLock {
        // When we first construct a negotiation, we initialize it to the requested state right away.
        // For later stages, we only allow the consumer to request new contract offers if we have already offered a
        // contract.
        // TODO: Are there any packet loss or reordering cases where this is a problem?
        ensureState(
            if (initial) {
                NegotiationState.REQUESTED
            } else {
                NegotiationState.OFFERED
            },
        )

        val parsedPolicy = OdrlPolicyParser().parse(req.offer)
        val (ok, counter) = negotiator.evaluateRequest(parsedPolicy)

        if (!ok) {
            if (counter != null) {
                sendMessage(
                    "$consumerPid/offers",
                    ContractOfferMessage(
                        providerPid = providerPid,
                        consumerPid = consumerPid,
                        callbackAddress = "TODO: Own address",
                        offer = jacksonObjectMapper().convertValue<JsonNode>(counter),
                    ),
                )
                state = NegotiationState.OFFERED
            } else {
                sendMessage(
                    "$consumerPid/termination",
                    ContractNegotiationTerminationMessage(
                        providerPid = providerPid,
                        consumerPid = consumerPid,
                        // TODO: While error codes are implementation defined, we may want to sync with the EDC
                        code = "noAgreement",
                        reason = listOf("No acceptable contract can be proposed for this negotiation"),
                    ),
                )
                state = NegotiationState.TERMINATED
                completed.cancel("No agreement can be found")
            }
        }

        // We have accepted the contract as is, tell the consumer and anyone waiting for this contract negotiation
        sendMessage(
            "$consumerPid/agreement",
            ContractAgreementMessage(
                providerPid = providerPid,
                consumerPid = consumerPid,
                // TODO: Convert to odrl agreement before encoding
                agreement = jacksonObjectMapper().convertValue<JsonNode>(parsedPolicy),
                callbackAddress = "TODO: Own address",
            ),
        )
        state = NegotiationState.AGREED
    }

    suspend fun processOffer(
        req: ContractOfferMessage,
        initial: Boolean = false,
    ) = mutex.withLock {
        ensureState(
            if (initial) {
                NegotiationState.OFFERED
            } else {
                NegotiationState.REQUESTED
            },
        )

        val parsedPolicy = OdrlPolicyParser().parse(req.offer)
        val (ok, newReq) = negotiator.evaluateOffer(parsedPolicy)

        if (!ok) {
            if (newReq != null) {
                state = NegotiationState.REQUESTED
                sendMessage(
                    "$providerPid/request",
                    ContractRequestMessage(
                        providerPid = providerPid,
                        consumerPid = consumerPid,
                        // TODO: Somehow get own address
                        callbackAddress = "",
                        offer = jacksonObjectMapper().convertValue(newReq),
                    ),
                )
                return
            } else {
                sendMessage(
                    "$providerPid/termination",
                    ContractNegotiationTerminationMessage(
                        providerPid = providerPid,
                        consumerPid = consumerPid,
                        code = "noAgreement",
                        reason = listOf("No acceptable contract can be derived from the offer"),
                    ),
                )
                state = NegotiationState.TERMINATED
                completed.cancel("No agreement can be reached")
            }
        }

        sendMessage(
            "$providerPid/events",
            ContractNegotiationEventMessage(
                providerPid = providerPid,
                consumerPid = consumerPid,
                eventType = NegotiationEvent.ACCEPTED,
            ),
        )
        state = NegotiationState.ACCEPTED
    }

    suspend fun getStateMessage(): ContractNegotiation =
        mutex.withLock {
            return ContractNegotiation(
                providerPid,
                consumerPid,
                state,
            )
        }
}
