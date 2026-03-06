package de.fraunhofer.aisec.glue.negotiation.builtin

import com.fasterxml.jackson.databind.ObjectMapper
import de.fraunhofer.aisec.glue.models.negotiation.*
import de.fraunhofer.aisec.glue.negotiation.Negotiation
import de.fraunhofer.aisec.glue.negotiation.edc.toEdcFormat
import de.fraunhofer.aisec.glue.negotiation.toAgreement
import de.fraunhofer.iosb.odrl.OdrlOffer
import de.fraunhofer.iosb.odrl.OdrlParty
import de.fraunhofer.iosb.odrl.OdrlPolicy
import de.fraunhofer.iosb.odrl.parser.OdrlParser
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.net.URI
import java.util.*

class BuiltinNegotiation(
    initialData: BuiltinNegotiationData,
    val httpClient: HttpClient,
    val negotiator: Negotiator,
    val objectMapper: ObjectMapper,
    private val odrlParser: OdrlParser,
    val ownIdentity: String,
    scope: CoroutineScope,
) : Negotiation {
    private val _data = MutableStateFlow(initialData)
    val data = _data.asStateFlow()

    private val messageQueue = Channel<Pair<DspMessage, CompletableDeferred<DspMessage?>>>(Channel.UNLIMITED)
    private val workerJob: Job

    init {
        workerJob =
            scope.launch(NegotiationContext(this) + CoroutineName("negotiation-$id-worker")) {
                dispatchMessages()
            }
    }

    override val id: String
        get() = _data.value.id.toString()

    override val role: Negotiation.Role
        get() = _data.value.role

    override val peerIdentity: String?
        get() = _data.value.peer

    private val internalState: NegotiationState
        get() = _data.value.state

    private val currentPolicy: OdrlPolicy?
        get() = _data.value.contract

    val peerPid: String
        get() = _data.value.peerPid

    private val ownEndpoint: Url
        get() = _data.value.callbackUrl

    private val peerEndpoint: Url
        get() = _data.value.peerUrl

    override val state: String
        get() = internalState.toString()

    private suspend fun dispatchMessages() {

        for ((message, response) in messageQueue) {
            when (message) {
                is ContractRequestMessage -> handleRequest(message, response)
                is ContractOfferMessage -> handleOffer(message, response)
                is ContractNegotiationEventMessage -> handleEvent(message, response)
                is ContractAgreementMessage -> handleAgreement(message, response)
                is ContractAgreementVerificationMessage -> handleVerify(message, response)
                is ContractNegotiationTerminationMessage -> handleTermination(message, response)
                else -> response.completeExceptionally(IllegalArgumentException("Unknown message type: ${message::class}"))
            }

            // Negotiation is done, we can exit the worker coroutine
            if (internalState in listOf(NegotiationState.FINALIZED, NegotiationState.TERMINATED)) {
                break
            }
        }
        for ((_, result) in messageQueue) {
            result.cancel("Negotiation has ${if (internalState == NegotiationState.FINALIZED) "completed" else "terminated"}")
        }
        messageQueue.close()
    }

    suspend fun submitMessage(message: DspMessage): DspMessage? {
        val response = CompletableDeferred<DspMessage?>()
        messageQueue.send(Pair(message, response))
        return response.await()
    }

    private fun updateState(updateAction: (BuiltinNegotiationData) -> BuiltinNegotiationData) {
        _data.update { old ->
            val new = updateAction(old)
            LOG.atTrace {
                message = "Updating negotiation state"
                payload =
                    mapOf(
                        "role" to role,
                        "id" to id,
                        "peerId" to peerPid,
                        "oldState" to old,
                        "newState" to new,
                    )
            }
            new
        }
    }

    /**
     * Ensures that the negotiation is in the right state before telling the caller to respond to the initial request.
     * This should be the right function for most interactions where the negotiation has to validate that the incoming
     * message can lead to a valid state transition and returning an error if not.
     */
    @Suppress("NOTHING_TO_INLINE")
    private inline fun precondition(
        result: CompletableDeferred<DspMessage?>,
        expectedRole: Negotiation.Role,
        vararg expectedStates: NegotiationState,
    ): Boolean {
        val ok = expectedStates.contains(internalState) && role == expectedRole
        if (!ok) {
            LOG.atDebug {
                message = "The received message is invalid for the current negotiation state"
                payload =
                    mapOf(
                        "role" to role,
                        "id" to id,
                        "peerId" to peerPid,
                        "state" to internalState,
                        "expectedStates" to expectedStates,
                        "expectedRole" to expectedRole,
                    )
            }
            val (providerPid, consumerPid) = getProviderAndConsumerPid()
            result.complete(
                ContractNegotiationError(
                    providerPid = providerPid,
                    consumerPid = consumerPid,
                    code = "invalidState",
                ),
            )
        }
        return ok
    }

    private suspend inline fun <reified T> sendMessage(
        endpoint: String,
        message: T,
        clientId: String? = null,
    ): HttpResponse {
        LOG.atDebug {
            this.message = "Sending message via POST request"
            payload =
                mapOf(
                    "endpoint" to endpoint,
                    "message" to message,
                )
        }
        val url =
            URLBuilder(peerEndpoint).appendEncodedPathSegments("negotiations", endpoint).build()

        // Build a custom Authorization header with JSON structure
        val authHeader =
            objectMapper.writeValueAsString(
                mapOf(
                    "region" to "eu",
                    "audience" to peerEndpoint.toString(),
                    "clientId" to (clientId ?: ownIdentity),
                ),
            )

        return httpClient.post(url) {
            contentType(ContentType.Application.Json)
            // Override default auth by setting custom Authorization header
            headers.remove(HttpHeaders.Authorization)
            header(HttpHeaders.Authorization, authHeader)
            setBody(message)
        }
    }

    private suspend fun terminateOnError(resp: HttpResponse): Boolean {
        if (!resp.status.isSuccess()) {
            val bodyText = resp.body<Map<String, Any>>()
            LOG.atDebug {
                message = "Received an error response from the peer"
                payload =
                    mapOf(
                        "statusCode" to resp.status.value,
                        "reasonPhrase" to resp.status.description,
                        "body" to bodyText,
                    )
            }
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
        updateState { it.copy(state = NegotiationState.TERMINATED, failureReason = "${reason ?: "Unknown Error"} ($code)") }
        val (providerPid, consumerPid) = getProviderAndConsumerPid()
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
     * Handle a contract request message from the consumer.
     * This is only valid if we are the provider and the negotiation is in its initial or the offered state.
     */
    private suspend fun handleRequest(
        req: ContractRequestMessage,
        response: CompletableDeferred<DspMessage?>,
    ) {
        if (!precondition(response, Negotiation.Role.PROVIDER, NegotiationState.INITIAL, NegotiationState.OFFERED)) {
            return
        }

        updateState { old -> old.copy(state = NegotiationState.REQUESTED) }
        response.complete(
            ContractNegotiation(
                providerPid = id,
                consumerPid = peerPid,
                state = NegotiationState.REQUESTED,
            ),
        )

        // Continue with processing
        val (ok, counter) = negotiator.evaluateRequest(req.offer)
        LOG.atDebug {
            message = "The negotiator validated the request"
            payload =
                mapOf(
                    "role" to role,
                    "id" to id,
                    "peerId" to peerPid,
                    "state" to internalState,
                    "ok" to ok,
                    "counter" to counter,
                )
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

                updateState { it.copy(contract = counter, state = NegotiationState.OFFERED) }
                return
            } else {
                terminateNegotiation("errPolicyFailure", "No counteroffer can be computed for this request")
                return
            }
        }

        val peerParty = OdrlParty(uid = peerIdentity?.let { URI.create("http://$it") } ?: peerEndpoint.toURI())
        val agreement = req.offer.toAgreement(odrlParser, peerParty)

        updateState { it.copy(contract = agreement, state = NegotiationState.AGREED) }
        terminateOnError(
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
    }

    /**
     * Process a contract offer sent from the provider.
     * This operation is only allowed if we are a consumer and the negotiation is either in its initial or the requested
     * state, which will be reported back by the argument to [responseCallback].
     */
    private suspend fun handleOffer(
        req: ContractOfferMessage,
        result: CompletableDeferred<DspMessage?>,
    ) {
        if (!precondition(result, Negotiation.Role.CONSUMER, NegotiationState.INITIAL, NegotiationState.REQUESTED)) {
            return
        }

        updateState { old -> old.copy(state = NegotiationState.OFFERED, contract = req.offer) }
        result.complete(
            ContractNegotiation(
                providerPid = peerPid,
                consumerPid = id,
                state = NegotiationState.OFFERED,
            ),
        )

        val (ok, counter) = negotiator.evaluateOffer(req.offer)
        LOG.atDebug {
            message = "The negotiator validated the offer"
            payload =
                mapOf(
                    "role" to role,
                    "id" to id,
                    "peerId" to peerPid,
                    "state" to internalState,
                    "ok" to ok,
                    "counter" to counter,
                )
        }

        if (!ok) {
            if (counter != null) {
                if (!terminateOnError(
                        sendMessage(
                            "$peerPid/request",
                            ContractRequestMessage(
                                uid = counter.uid.toEdcFormat(),
                                providerPid = peerPid,
                                consumerPid = id,
                                callbackAddress = ownEndpoint.toString(),
                                offer = counter,
                            ),
                        ),
                    )
                ) {
                    return
                }
                updateState { it.copy(contract = counter, state = NegotiationState.REQUESTED) }
                return
            } else {
                terminateNegotiation("errPolicyFailure", "No counteroffer can be computed for this request")
            }
        }

        updateState { it.copy(state = NegotiationState.ACCEPTED, contract = req.offer) }
        terminateOnError(
            sendMessage(
                "$peerPid/events",
                ContractNegotiationEventMessage(
                    providerPid = peerPid,
                    consumerPid = id,
                    eventType = NegotiationEvent.ACCEPTED,
                ),
            ),
        )
    }

    private suspend fun handleEvent(
        req: ContractNegotiationEventMessage,
        result: CompletableDeferred<DspMessage?>,
    ) {
        // The response to events is different based on our role.
        // Providers only receive accept events, consumers receive finished events.
        when (role) {
            Negotiation.Role.PROVIDER -> handleAccept(req, result)
            Negotiation.Role.CONSUMER -> handleFinalize(req, result)
        }
    }

    private suspend fun handleAccept(
        req: ContractNegotiationEventMessage,
        result: CompletableDeferred<DspMessage?>,
    ) {
        // By making sure that the state machine is in the offered state, we make sure that we have made the current offer,
        // as client offers (= requests) move us into the requested state.
        if (!precondition(result, Negotiation.Role.PROVIDER, NegotiationState.OFFERED)) {
            return
        }

        updateState { it.copy(state = NegotiationState.ACCEPTED) }
        result.complete(null)

        val agreement = (currentPolicy as OdrlOffer).toAgreement(odrlParser, OdrlParty())

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
        updateState { it.copy(contract = agreement, state = NegotiationState.AGREED) }
    }

    private suspend fun handleVerify(
        req: ContractAgreementVerificationMessage,
        result: CompletableDeferred<DspMessage?>,
    ) {
        if (!precondition(result, Negotiation.Role.PROVIDER, NegotiationState.AGREED)) {
            return
        }

        updateState { it.copy(state = NegotiationState.VERIFIED) }
        result.complete(null)

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
            //
            return
        }

        updateState { it.copy(state = NegotiationState.FINALIZED) }
    }

    private fun handleFinalize(
        req: ContractNegotiationEventMessage,
        result: CompletableDeferred<DspMessage?>,
    ) {
        if (!precondition(result, Negotiation.Role.CONSUMER, NegotiationState.VERIFIED)) {
            return
        }

        updateState { it.copy(state = NegotiationState.FINALIZED) }
        result.complete(null)
    }

    fun getStateMessage(): ContractNegotiation {
        val (providerPid, consumerPid) = getProviderAndConsumerPid()

        return ContractNegotiation(
            providerPid = providerPid,
            consumerPid = consumerPid,
            state = internalState,
        )
    }

    private suspend fun handleAgreement(
        req: ContractAgreementMessage,
        result: CompletableDeferred<DspMessage?>,
    ) {
        if (!precondition(result, Negotiation.Role.CONSUMER, NegotiationState.REQUESTED, NegotiationState.ACCEPTED)) {
            return
        }

        //       Should this be passed to the negotiator as well?

        // If the provider tries to send a (semantically) different agreement, reject and terminate the negotiation.
        // We leave the semantics to the negotiator :)
        if (!negotiator.verifyAgreement(currentPolicy as OdrlOffer, req.agreement)) {
            LOG.atDebug {
                message = "The negotiator deems this agreement is not the same as the previous policy"
                payload =
                    mapOf(
                        "role" to role,
                        "id" to id,
                        "peerId" to peerPid,
                        "state" to internalState,
                    )
            }
            result.complete(
                ContractNegotiationError(
                    providerPid = peerPid,
                    consumerPid = id,
                    code = "errPolicyFailure",
                ),
            )
            return
        }

        updateState { it.copy(contract = req.agreement, state = NegotiationState.AGREED) }
        result.complete(null)

        if (!terminateOnError(
                sendMessage(
                    "$peerPid/agreement/verification",
                    ContractAgreementVerificationMessage(
                        providerPid = peerPid,
                        consumerPid = id,
                        uid = UUID.randomUUID().toString(),
                    ),
                ),
            )
        ) {
            return
        }

        updateState { it.copy(state = NegotiationState.VERIFIED) }
    }

    private fun handleTermination(
        req: ContractNegotiationTerminationMessage,
        result: CompletableDeferred<DspMessage?>,
    ) {
        val messageBuilder = StringBuilder("The peer terminated the negotiation with code ${req.code}")
        if (req.reason != null) {
            for (reason in req.reason) {
                messageBuilder.append(": $reason")
            }
        }
        updateState { it.copy(state = NegotiationState.TERMINATED, failureReason = messageBuilder.toString()) }
        result.complete(null)
    }

    override suspend fun cancel(cause: String?) {
        //  termination message, but what do we do if the worker is stopped?
        terminateNegotiation("errCancelled")
    }

    companion object {
        private val LOG = KotlinLogging.logger {}
    }
}
