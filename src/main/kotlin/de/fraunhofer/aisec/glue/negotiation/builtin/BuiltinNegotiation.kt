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
import java.nio.ByteBuffer
import java.util.*

/**
 * The main driver for the builtin DSP negotiation process.
 * The negotiation processes runs in a separate coroutine that accepts incoming messages with the [submitMessage]
 * function.
 * The state of the negotiation is observable via the [data] flow.
 */
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

    // TODO: Is it ok to throw this exception here?
    val peerPid: String
        get() = _data.value.peerPid

    private val ownEndpoint: Url
        get() = _data.value.callbackUrl

    private val peerEndpoint: Url
        get() = _data.value.peerUrl

    override val state: String
        get() = internalState.toString()

    private suspend fun recover() {
        LOG.atDebug {
            message = "Attempting to recover the negotiation process from data"
            payload =
                mapOf(
                    "id" to id,
                    "role" to role,
                    "peerId" to peerPid,
                    "state" to state,
                )
        }

        when (Pair(internalState, role)) {
            Pair(NegotiationState.REQUESTED, Negotiation.Role.PROVIDER) -> handleRequestInner()
            Pair(NegotiationState.OFFERED, Negotiation.Role.CONSUMER) -> handleOfferInner()
            Pair(NegotiationState.ACCEPTED, Negotiation.Role.PROVIDER) -> handleAcceptInner()
            Pair(NegotiationState.AGREED, Negotiation.Role.CONSUMER) -> handleAgreementInner()
            Pair(NegotiationState.VERIFIED, Negotiation.Role.PROVIDER) -> handleVerifyInner()
            else ->
                LOG.atTrace {
                    message = "No recovery action required"
                    payload =
                        mapOf(
                            "id" to id,
                            "role" to role,
                            "peerId" to peerPid,
                            "state" to state,
                        )
                }
        }
    }

    private suspend fun dispatchMessages() {
        if (internalState != NegotiationState.INITIAL) {
            recover()
        }

        while (internalState !in listOf(NegotiationState.FINALIZED, NegotiationState.TERMINATED)) {
            val (req, response) = messageQueue.receive()

            if (req.id == data.value.lastReceivedMessage) {
                LOG.atDebug {
                    message = "Ignoring likely duplicate message"
                    payload =
                        mapOf(
                            "role" to role,
                            "id" to id,
                            "peerId" to peerPid,
                            "message" to req,
                        )
                }
                response.complete(null)
                continue
            }

            when (req) {
                is ContractRequestMessage -> handleRequest(req, response)
                is ContractOfferMessage -> handleOffer(req, response)
                is ContractNegotiationEventMessage -> handleEvent(req, response)
                is ContractAgreementMessage -> handleAgreement(req, response)
                is ContractAgreementVerificationMessage -> handleVerify(req, response)
                is ContractNegotiationTerminationMessage -> handleTermination(req, response)
                else -> response.completeExceptionally(IllegalArgumentException("Unknown message type: ${req::class}"))
            }
        }

        messageQueue.close()
        while (true) {
            val (_, result) = messageQueue.tryReceive().getOrNull() ?: break
            result.cancel("Negotiation has ${if (internalState == NegotiationState.FINALIZED) "completed" else "terminated"}")
        }
    }

    /**
     * Submit a received contract negotiation protocol message to the negotiation state machine.
     * When the message is processed, returns a response message or null if no message is to be sent.
     * If the returned value is of type [ContractNegotiationError], an appropriate transport protocol error (e.g. HTTP status 400)
     * must be returned to the caller.
     * Received messages are processed in order of arrival.
     */
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
    private suspend inline fun precondition(
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
                    id = generateNextMessageId(),
                    providerPid = providerPid,
                    consumerPid = consumerPid,
                    code = "invalidState",
                ),
            )
            terminateNegotiation("invalidState", "The message is invalid in the current state")
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
        // TODO: We should rather handle auth in a dedicated http client plugin
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
        updateState {
            it.copy(
                state = NegotiationState.TERMINATED,
                failureReason = "${reason ?: "Unknown Error"} ($code)",
                nextSentMessage = it.nextSentMessage + 1,
            )
        }
        val (providerPid, consumerPid) = getProviderAndConsumerPid()
        sendMessage(
            "$peerPid/termination",
            ContractNegotiationTerminationMessage(
                id = generateNextMessageId(),
                providerPid = providerPid,
                consumerPid = consumerPid,
                code = code,
                reason = reason?.let(::listOf),
            ),
        )
    }

    private fun generateNextMessageId(): String {
        return generateNextMessageId(data.value.id, data.value.nextSentMessage)
    }

    fun getStateMessage(): ContractNegotiation {
        val (providerPid, consumerPid) = getProviderAndConsumerPid()

        return ContractNegotiation(
            id = UUID.randomUUID().toString(),
            providerPid = providerPid,
            consumerPid = consumerPid,
            state = internalState,
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

        response.complete(
            ContractNegotiation(
                id = generateNextMessageId(),
                providerPid = id,
                consumerPid = peerPid,
                state = NegotiationState.REQUESTED,
            ),
        )
        updateState { old ->
            old.copy(
                state = NegotiationState.REQUESTED,
                contract = req.offer,
                lastReceivedMessage = req.id,
                nextSentMessage = old.nextSentMessage + 1,
            )
        }

        handleRequestInner()
    }

    private suspend fun handleRequestInner() {
        // Continue with processing
        val offer = data.value.contract as? OdrlOffer ?: throw AssertionError("handleRequestInner called without an offer")
        val (ok, counter) = negotiator.evaluateRequest(offer)
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
                                id = generateNextMessageId(),
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

                updateState { it.copy(contract = counter, state = NegotiationState.OFFERED, nextSentMessage = it.nextSentMessage + 1) }
                return
            } else {
                terminateNegotiation("errPolicyFailure", "No counteroffer can be computed for this request")
                return
            }
        }

        // TODO: Fill in true assignee here
        val peerParty = OdrlParty(uid = peerIdentity?.let { URI.create("http://$it") } ?: peerEndpoint.toURI())
        val agreement = offer.toAgreement(odrlParser, peerParty)

        terminateOnError(
            sendMessage(
                "$peerPid/agreement",
                ContractAgreementMessage(
                    id = generateNextMessageId(),
                    providerPid = id,
                    consumerPid = peerPid,
                    agreement = agreement,
                    callbackAddress = ownEndpoint.toString(),
                ),
            ),
        )
        updateState { it.copy(contract = agreement, state = NegotiationState.AGREED, nextSentMessage = it.nextSentMessage + 1) }
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

        result.complete(
            ContractNegotiation(
                id = generateNextMessageId(),
                providerPid = peerPid,
                consumerPid = id,
                state = NegotiationState.OFFERED,
            ),
        )
        updateState { old ->
            old.copy(
                state = NegotiationState.OFFERED,
                contract = req.offer,
                lastReceivedMessage = req.id,
                nextSentMessage = old.nextSentMessage + 1,
            )
        }
        handleOfferInner()
    }

    private suspend fun handleOfferInner() {
        val offer = data.value.contract as? OdrlOffer ?: throw AssertionError("handleOfferInner called without an offer")
        val (ok, counter) = negotiator.evaluateOffer(offer)
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
                                // TODO: Is this needed?
                                id = counter.uid.toEdcFormat(),
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
                updateState { it.copy(contract = counter, state = NegotiationState.REQUESTED, nextSentMessage = it.nextSentMessage + 1) }
                return
            } else {
                terminateNegotiation("errPolicyFailure", "No counteroffer can be computed for this request")
            }
        }

        terminateOnError(
            sendMessage(
                "$peerPid/events",
                ContractNegotiationEventMessage(
                    id = generateNextMessageId(),
                    providerPid = peerPid,
                    consumerPid = id,
                    eventType = NegotiationEvent.ACCEPTED,
                ),
            ),
        )
        updateState { it.copy(state = NegotiationState.ACCEPTED, contract = offer, nextSentMessage = it.nextSentMessage + 1) }
    }

    private suspend fun handleEvent(
        req: ContractNegotiationEventMessage,
        result: CompletableDeferred<DspMessage?>,
    ) {
        // The response to events is different based on our role.
        // Providers only receive accept events, consumers receive finished events.
        when (req.eventType) {
            NegotiationEvent.ACCEPTED -> handleAccept(req, result)
            NegotiationEvent.FINALIZED -> handleFinalize(req, result)
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

        result.complete(null)
        updateState { it.copy(state = NegotiationState.ACCEPTED, lastReceivedMessage = req.id) }
        handleAcceptInner()
    }

    private suspend fun handleAcceptInner() {
        // TODO: Fill in true assignee here
        val agreement = (currentPolicy as OdrlOffer).toAgreement(odrlParser, OdrlParty())

        if (!terminateOnError(
                sendMessage(
                    "$peerPid/agreement",
                    ContractAgreementMessage(
                        id = generateNextMessageId(),
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
        updateState { it.copy(contract = agreement, state = NegotiationState.AGREED, nextSentMessage = it.nextSentMessage + 1) }
    }

    private suspend fun handleVerify(
        req: ContractAgreementVerificationMessage,
        result: CompletableDeferred<DspMessage?>,
    ) {
        if (!precondition(result, Negotiation.Role.PROVIDER, NegotiationState.AGREED)) {
            return
        }

        result.complete(null)
        updateState { it.copy(state = NegotiationState.VERIFIED, lastReceivedMessage = req.id) }
        handleVerifyInner()
    }

    private suspend fun handleVerifyInner() {
        if (!terminateOnError(
                sendMessage(
                    "$peerPid/events",
                    ContractNegotiationEventMessage(
                        id = generateNextMessageId(),
                        providerPid = id,
                        consumerPid = peerPid,
                        eventType = NegotiationEvent.FINALIZED,
                    ),
                ),
            )
        ) {
            return
        }

        updateState { it.copy(state = NegotiationState.FINALIZED, nextSentMessage = it.nextSentMessage + 1) }
    }

    private suspend fun handleFinalize(
        req: ContractNegotiationEventMessage,
        result: CompletableDeferred<DspMessage?>,
    ) {
        if (!precondition(result, Negotiation.Role.CONSUMER, NegotiationState.VERIFIED)) {
            return
        }

        result.complete(null)
        updateState { it.copy(state = NegotiationState.FINALIZED, lastReceivedMessage = req.id) }
    }

    private suspend fun handleAgreement(
        req: ContractAgreementMessage,
        result: CompletableDeferred<DspMessage?>,
    ) {
        if (!precondition(result, Negotiation.Role.CONSUMER, NegotiationState.REQUESTED, NegotiationState.ACCEPTED)) {
            return
        }

        // TODO: (how) should we evaluate the timestamp? What would be a good timeout to detect outdated requests?
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
                    id = generateNextMessageId(),
                    providerPid = peerPid,
                    consumerPid = id,
                    code = "errPolicyFailure",
                ),
            )
            // TODO: Terminate without an extra message?
            terminateNegotiation("errPolicyFailure", "The agreement does not match the previous offer")
            return
        }

        result.complete(null)
        updateState { it.copy(contract = req.agreement, state = NegotiationState.AGREED, lastReceivedMessage = req.id) }
        handleAgreementInner()
    }

    private suspend fun handleAgreementInner() {
        if (!terminateOnError(
                sendMessage(
                    "$peerPid/agreement/verification",
                    ContractAgreementVerificationMessage(
                        id = generateNextMessageId(),
                        providerPid = peerPid,
                        consumerPid = id,
                    ),
                ),
            )
        ) {
            return
        }

        updateState { it.copy(state = NegotiationState.VERIFIED, nextSentMessage = it.nextSentMessage + 1) }
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
        result.complete(null)
        updateState { it.copy(state = NegotiationState.TERMINATED, failureReason = messageBuilder.toString()) }
    }

    override suspend fun cancel(cause: String?) {
        // TODO: This must somehow flow through the worker job as well. The easiest way would be to just enqueue a
        //  termination message, but what do we do if the worker is stopped?
        if (workerJob.isActive) {
            submitMessage(
                ContractNegotiationTerminationMessage(
                    id = "",
                    providerPid = "",
                    consumerPid = "",
                    code = "errCancelled",
                    reason = cause?.let(::listOf),
                ),
            )
        } else {
            LOG.atDebug {
                message = "The worker job is not active, trying to cancel directly"
                payload =
                    mapOf(
                        "id" to id,
                        "role" to role,
                        "peerId" to peerPid,
                        "state" to internalState,
                        "cause" to cause,
                    )
            }
            terminateNegotiation("errCancelled")
        }
    }

    companion object {
        private val LOG = KotlinLogging.logger {}

        internal fun generateNextMessageId(
            id: UUID,
            index: Int,
        ): String {
            val buffer = ByteBuffer.allocate(20)
            buffer.putLong(id.mostSignificantBits)
            buffer.putLong(id.leastSignificantBits)
            buffer.putInt(index)
            return UUID.nameUUIDFromBytes(buffer.array()).toString()
        }
    }
}
