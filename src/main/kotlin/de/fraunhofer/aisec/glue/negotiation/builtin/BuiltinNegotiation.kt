package de.fraunhofer.aisec.glue.negotiation.builtin

import com.fasterxml.jackson.databind.ObjectMapper
import de.fraunhofer.aisec.glue.models.negotiation.*
import de.fraunhofer.aisec.glue.negotiation.Negotiation
import de.fraunhofer.aisec.glue.negotiation.NegotiationCompletionCallback
import de.fraunhofer.aisec.glue.negotiation.NegotiationException
import de.fraunhofer.aisec.glue.negotiation.toAgreement
import de.fraunhofer.iosb.odrl.OdrlAgreement
import de.fraunhofer.iosb.odrl.OdrlOffer
import de.fraunhofer.iosb.odrl.OdrlParty
import de.fraunhofer.iosb.odrl.OdrlPolicy
import de.fraunhofer.iosb.odrl.parser.OdrlParser
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class BuiltinNegotiation(
    state: NegotiationState,
    // TODO: Don't save this as string and don't make it nullable or mutable
    override var peerIdentity: String?,
    var currentPolicy: OdrlPolicy? = null,
    override val role: Negotiation.Role,
    override val id: String,
    val peerPid: String,
    val ownEndpoint: Url,
    val peerEndpoint: Url,
    val httpClient: HttpClient,
    val negotiator: Negotiator,
    val objectMapper: ObjectMapper,
    val completionCallback: NegotiationCompletionCallback,
    private val odrlParser: OdrlParser,
) : Negotiation {
    var internalState: NegotiationState = state
        private set(newState) {
            LOG.atDebug {
                message = "Updating negotiation state"
                val (providerPid, consumerPid) = getProviderAndConsumerPid()
                payload =
                    mapOf(
                        "role" to role,
                        "providerPid" to providerPid,
                        "consumerPid" to consumerPid,
                        "oldState" to field,
                        "newState" to newState,
                    )
            }
            field = newState
        }
    private val mutex = Mutex()

    override val state: String
        get() = internalState.toString()

    /**
     * Ensures that the negotiation is in the right state before telling the caller to respond to the initial request.
     * This should be the right function for most interactions where the negotiation has to validate that the incoming
     * message can lead to a valid state transition and returning an error if not.
     */
    @Suppress("NOTHING_TO_INLINE")
    private inline fun precondition(
        expectedState: NegotiationState,
        expectedRole: Negotiation.Role,
    ): Boolean {
        val ok = internalState == expectedState && role == expectedRole
        if (!ok) {
            LOG.atDebug {
                message = "The received message is invalid for the current negotiation state"
                val (providerPid, consumerPid) = getProviderAndConsumerPid()
                payload =
                    mapOf(
                        "role" to role,
                        "providerPid" to providerPid,
                        "consumerPid" to consumerPid,
                        "state" to internalState,
                        "expectedState" to expectedState,
                        "expectedRole" to expectedRole,
                    )
            }
        }
        return ok
    }

    private suspend inline fun <reified T> sendMessage(
        endpoint: String,
        message: T,
    ): HttpResponse {
        val url = URLBuilder(peerEndpoint).appendEncodedPathSegments(endpoint).build()
        return httpClient.post(url) {
            contentType(ContentType.Application.Json)
            setBody(message)
        }
    }

    private suspend fun terminateOnError(resp: HttpResponse): Boolean {
        if (!resp.status.isSuccess()) {
            terminateNegotiation("errInvalidResponse", "Received an invalid response")
            return false
        }
        return true
    }

    /**
     * Derive (providerPid, consumerPid) from own and peer pid and role.
     */
    private fun getProviderAndConsumerPid(): Pair<String, String> {
        return if (role == Negotiation.Role.PROVIDER) {
            Pair(id, peerPid)
        } else {
            Pair(peerPid, id)
        }
    }

    /**
     * Send a termination message to the peer containing the stated code and reason (if any)
     */
    private suspend fun terminateNegotiation(
        code: String,
        reason: String? = null,
    ) {
        // Note: The spec technically disallows us from terminating in some states, but it's probably better to not
        // care and terminate anyway.
        // What's the worst that can happen? The peer terminating the connection for a protocol violation? :)
        internalState = NegotiationState.TERMINATED
        val (providerPid, consumerPid) = getProviderAndConsumerPid()
        LOG.atDebug {
            message = "Negotiation has been terminated by the application, notifying peer"
            payload =
                mapOf(
                    "role" to role,
                    "state" to internalState,
                    "providerPid" to providerPid,
                    "consumerPid" to consumerPid,
                    "code" to code,
                    "reason" to reason,
                )
        }
        completionCallback(Result.failure(NegotiationException(reason ?: "Unknown error")))
        sendMessage(
            "$peerPid/termination",
            ContractNegotiationTerminationMessage(
                providerPid = providerPid,
                consumerPid = consumerPid,
                code = code,
                reason = reason?.let(::listOf),
            ),
        )
    }

    /**
     * Report completion of the negotiation and the resulting policy.
     */
    private suspend fun completeNegotiation() {
        internalState = NegotiationState.FINALIZED
        val (providerPid, consumerPid) = getProviderAndConsumerPid()
        LOG.atDebug {
            message = "Negotiation completed successfully"
            payload = mapOf("role" to role, "state" to internalState, "providerPid" to providerPid, "consumerPid" to consumerPid)
        }
        // The non-null assertion here is safe since the negotiation may only be completed with a contract
        completionCallback(Result.success(currentPolicy ?: throw IllegalStateException("No policy available")))
    }

    /**
     * Process a contract request message from the consumer.
     * This is only valid if we are the provider and the negotiation is in its initial or the offered state, which will
     * be indicated by the parameter of [responseCallback] when invoked.
     */
    suspend fun processRequest(
        req: ContractRequestMessage,
        initial: Boolean = false,
        responseCallback: suspend (Boolean) -> Unit,
    ) = mutex.withLock {
        // When we first construct a negotiation, we initialize it to the requested state right away.
        // For later stages, we only allow the consumer to request new contract offers if we have already offered a
        // contract.
        // TODO: Maybe add an initial state to NegotiationState instead of this and the initial paramter
        val allowedState =
            if (initial) {
                NegotiationState.REQUESTED
            } else {
                NegotiationState.OFFERED
            }
        if (!precondition(allowedState, Negotiation.Role.PROVIDER)) {
            responseCallback(false)
            return
        }

        responseCallback(true)

        val (ok, counter) = negotiator.evaluateRequest(req.offer)
        LOG.atDebug {
            message = "Negotiator evaluates the request as ${if (ok) {
                "acceptable"
            } else {
                "not acceptable"
            }}"
            val (providerPid, consumerPid) = getProviderAndConsumerPid()
            payload = mapOf<String, Any>("role" to role, "state" to internalState, "providerPid" to providerPid, "consumerPid" to consumerPid)
        }

        if (!ok) {
            if (counter != null) {
                if (!terminateOnError(
                        sendMessage(
                            "$peerPid/offers",
                            ContractOfferMessage(
                                providerPid = id,
                                consumerPid = peerPid,
                                callbackAddress = ownEndpoint.toString(),
                                offer = counter,
                            ),
                        ),
                    )
                ) {
                    return
                }
                // TODO: This will not work
                currentPolicy = counter
                internalState = NegotiationState.OFFERED
                return
            } else {
                terminateNegotiation("errPolicyFailure", "No counteroffer can be computed for this request")
                return
            }
        }

        // TODO: Fill in true assignee here
        val agreement = req.offer.toAgreement(odrlParser, OdrlParty())

        // We have accepted the contract as is, tell the consumer and anyone waiting for this contract negotiation
        if (!terminateOnError(
                sendMessage(
                    "$peerPid/agreement",
                    ContractAgreementMessage(
                        providerPid = id,
                        consumerPid = peerPid,
                        agreement = agreement,
                        callbackAddress = ownEndpoint.toString(),
                    ),
                ),
            )
        ) {
            return
        }
        currentPolicy = agreement
        internalState = NegotiationState.AGREED
    }

    /**
     * Process a contract offer sent from the provider.
     * This operation is only allowed if we are a consumer and the negotiation is either in its initial or the requested
     * state, which will be reported back by the argument to [responseCallback].
     */
    suspend fun processOffer(
        req: ContractOfferMessage,
        initial: Boolean = false,
        responseCallback: suspend (Boolean) -> Unit,
    ) = mutex.withLock {
        val allowedState =
            if (initial) {
                NegotiationState.OFFERED
            } else {
                NegotiationState.REQUESTED
            }

        if (!precondition(allowedState, Negotiation.Role.CONSUMER)) {
            responseCallback(false)
            return
        }

        responseCallback(true)

        val (ok, newOffer) = negotiator.evaluateOffer(req.offer)
        LOG.atDebug {
            message = "Negotiator evaluates the offer as ${if (ok) {
                "acceptable"
            } else {
                "not acceptable"
            }}"
            val (providerPid, consumerPid) = getProviderAndConsumerPid()
            payload = mapOf<String, Any>("role" to role, "state" to internalState, "providerPid" to providerPid, "consumerPid" to consumerPid)
        }

        if (!ok) {
            if (newOffer != null) {
                sendMessage(
                    "$peerPid/request",
                    ContractRequestMessage(
                        providerPid = peerPid,
                        consumerPid = id,
                        callbackAddress = ownEndpoint.toString(),
                        offer = newOffer,
                    ),
                )
                internalState = NegotiationState.REQUESTED
                currentPolicy = newOffer
                return
            } else {
                terminateNegotiation("errPolicyFailure", "No counteroffer can be computed for this request")
                return
            }
        }

        currentPolicy = req.offer
        if (!terminateOnError(
                sendMessage(
                    "$peerPid/events",
                    ContractNegotiationEventMessage(
                        providerPid = peerPid,
                        consumerPid = id,
                        eventType = NegotiationEvent.ACCEPTED,
                    ),
                ),
            )
        ) {
            return
        }
        internalState = NegotiationState.ACCEPTED
    }

    suspend fun processAccept(responseCallback: suspend (Boolean) -> Unit): Unit =
        mutex.withLock {
            // By making sure that the state machine is in the offered state, we make sure that the current offer has
            // been made by us, as client offers (= requests) move us into the requested state.
            if (!precondition(NegotiationState.OFFERED, Negotiation.Role.PROVIDER)) {
                responseCallback(false)
                return
            }
            responseCallback(true)

            internalState = NegotiationState.ACCEPTED

            // TODO: Fill in true assignee here
            val agreement = (currentPolicy as OdrlOffer).toAgreement(odrlParser, OdrlParty())
            currentPolicy = agreement

            if (!terminateOnError(
                    sendMessage(
                        "$peerPid/agreement",
                        ContractAgreementMessage(
                            providerPid = id,
                            consumerPid = peerPid,
                            agreement = currentPolicy as OdrlAgreement,
                            callbackAddress = ownEndpoint.toString(),
                        ),
                    ),
                )
            ) {
                return
            }
            internalState = NegotiationState.AGREED
        }

    suspend fun processVerify(responseCallback: suspend (Boolean) -> Unit): Unit =
        mutex.withLock {
            if (!precondition(NegotiationState.AGREED, Negotiation.Role.PROVIDER)) {
                responseCallback(false)
                return
            }

            completeNegotiation()

            if (!terminateOnError(
                    sendMessage(
                        "$peerPid/events",
                        ContractNegotiationEventMessage(
                            providerPid = id,
                            consumerPid = peerPid,
                            eventType = NegotiationEvent.FINALIZED,
                        ),
                    ),
                )
            ) {
                // This will be after the terminate message, but we should probably still respond with false
                responseCallback(false)
                return
            }

            responseCallback(true)
        }

    suspend fun processFinalize(responseCallback: suspend (Boolean) -> Unit) {
        if (!precondition(NegotiationState.VERIFIED, Negotiation.Role.CONSUMER)) {
            responseCallback(false)
            return
        }

        completeNegotiation()
        responseCallback(true)
    }

    fun getStateMessageUnlocked(): ContractNegotiation {
        val (providerPid, consumerPid) = getProviderAndConsumerPid()

        return ContractNegotiation(
            providerPid = providerPid,
            consumerPid = consumerPid,
            state = internalState,
        )
    }

    suspend fun processAgreement(
        req: ContractAgreementMessage,
        responseCallback: suspend (Boolean) -> Unit,
    ): Unit =
        mutex.withLock {
            if ((internalState != NegotiationState.REQUESTED && internalState != NegotiationState.ACCEPTED) || role != Negotiation.Role.CONSUMER) {
                LOG.atDebug {
                    message = "The received message is invalid for the current negotiation state"
                    val (providerPid, consumerPid) = getProviderAndConsumerPid()
                    payload =
                        mapOf(
                            "role" to role,
                            "providerPid" to providerPid,
                            "consumerPid" to consumerPid,
                            "state" to internalState,
                            "expectedState" to "${NegotiationState.REQUESTED} or ${NegotiationState.ACCEPTED}",
                            "expectedRole" to Negotiation.Role.CONSUMER,
                        )
                }
                responseCallback(false)
                return
            }

            // TODO: (how) should we evaluate the timestamp? What would be a good timeout to detect outdated requests?
            //       Should this be passed to the negotiator as well?

            // If the provider tries to send a (semantically) different agreement, reject and terminate the negotiation.
            // We leave the semantics to the negotiator :)
            if (!negotiator.verifyAgreement(currentPolicy as OdrlOffer, req.agreement)) {
                LOG.atDebug {
                    message = "The negotiator deems this agreement is not the same as the previous policy"
                    val (providerPid, consumerPid) = getProviderAndConsumerPid()
                    payload = mapOf("role" to role, "providerPid" to providerPid, "consumerPid" to consumerPid, "state" to internalState)
                }
                responseCallback(false)
                terminateNegotiation("errPolicyFailure")
                return
            }

            responseCallback(true)
            currentPolicy = req.agreement
            internalState = NegotiationState.VERIFIED
            sendMessage(
                "$peerPid/agreement/verification",
                ContractAgreementVerificationMessage(
                    providerPid = peerPid,
                    consumerPid = id,
                ),
            )
        }

    suspend fun processTermination(
        req: ContractNegotiationTerminationMessage,
        responseCallback: suspend () -> Unit,
    ) = mutex.withLock {
        internalState = NegotiationState.TERMINATED
        val messageBuilder = StringBuilder("The peer terminated the negotiation with code ${req.code}")
        if (req.reason != null) {
            for (reason in req.reason) {
                messageBuilder.append(": $reason")
            }
        }
        completionCallback(Result.failure(NegotiationException(messageBuilder.toString())))
        responseCallback()
    }

    override suspend fun cancel(cause: String?) {
        mutex.withLock { terminateNegotiation("errCancelled") }
    }

    companion object {
        private val LOG = KotlinLogging.logger {}
    }
}
