package de.fraunhofer.aisec.glue.negotiation

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.convertValue
import de.fraunhofer.aisec.glue.models.negotiation.*
import fhg.glue.odrl.OdrlException
import fhg.glue.odrl.OdrlPolicy
import fhg.glue.odrl.OdrlPolicyParser
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.format.DateTimeFormatter

class Negotiation(
    var state: NegotiationState,
    // TODO: Don't save this as string and don't make it nullable or mutable
    var peerIdentity: String?,
    var currentPolicy: OdrlPolicy?,
    val role: Role,
    val ownPid: String,
    val peerPid: String,
    val ownEndpoint: Url,
    val peerEndpoint: Url,
    val httpClient: HttpClient,
    val negotiator: Negotiator,
    val objectMapper: ObjectMapper,
) {
    val completed = CompletableDeferred<OdrlPolicy>(null)
    private val mutex = Mutex()

    init {
        completed.invokeOnCompletion { cause ->
            // When the application cancels the negotiation (e.g. due to timeout) we notify our peer with a cancellation
            // message.
            // Note that this does not include cancellation due to errors.
            if (cause is CancellationException) {
                CoroutineScope(Dispatchers.Default).launch {
                    mutex.withLock { terminateNegotiation("errCancelled") }
                }
            }
        }
    }

    /**
     * Ensures that the negotiation is in the right state before telling the caller to respond to the initial request.
     * This should be the right function for most interactions where the negotiation has to validate that the incoming
     * message can lead to a valid state transition and returning an error if not.
     */
    private suspend fun ensurePreconditions(
        expectedState: NegotiationState,
        expectedRole: Role,
        cb: suspend (Boolean) -> Unit,
    ): Boolean {
        val ok = state == expectedState && role == expectedRole
        cb(ok)
        return ok
    }

    private suspend inline fun <reified T> sendMessage(
        endpoint: String,
        message: T,
    ): HttpResponse {
        val url =
            URLBuilder(peerEndpoint).apply {
                pathSegments = pathSegments + endpoint.split('/').map { s -> s.trim('/') }
            }.build()

        return httpClient.post(url) {
            contentType(ContentType.Application.Json)
            setBody(message)
        }
    }

    /**
     * Derive (providerPid, consumerPid) from own and peer pid and role.
     */
    private fun getProviderAndConsumerPid(): Pair<String, String> {
        return if (role == Role.PROVIDER) {
            Pair(ownPid, peerPid)
        } else {
            Pair(peerPid, ownPid)
        }
    }

    /**
     * Send a termination message to the peer containing the stated code and reason (if any)
     * TODO: Inline?
     */
    private suspend fun terminateNegotiation(
        code: String,
        reason: String? = null,
    ) {
        LOG.debug("Negotiation has been terminated by the application, notify peer")
        // Note: The spec technically disallows us from terminating in some states, but it's probably better to not
        // care and terminate anyway.
        // What's the worst that can happen? The peer terminating the connection for a protocol violation? :)
        state = NegotiationState.TERMINATED
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
     * Report completion of the negotiation and the resulting policy.
     */
    private fun completeNegotiation() {
        LOG.debug("Negotiation completed")
        completed.complete(currentPolicy!!)
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
        if (!ensurePreconditions(allowedState, Role.PROVIDER, responseCallback)) {
            return
        }

        val parsedPolicy = OdrlPolicyParser().parse(req.offer)
        val (ok, counter) = negotiator.evaluateRequest(parsedPolicy)

        if (!ok) {
            if (counter != null) {
                sendMessage(
                    "$peerPid/offers",
                    ContractOfferMessage(
                        providerPid = ownPid,
                        consumerPid = peerPid,
                        callbackAddress = ownEndpoint.toString(),
                        offer = objectMapper.convertValue<JsonNode>(counter),
                    ),
                )
                currentPolicy = counter
                state = NegotiationState.OFFERED
                return
            } else {
                terminateNegotiation("errPolicyFailure", "No counteroffer can be computed for this request")
                completed.completeExceptionally(
                    NegotiationException("No counteroffer can be computed for the peer's request"),
                )
                return
            }
        }

        // We have accepted the contract as is, tell the consumer and anyone waiting for this contract negotiation
        sendMessage(
            "$peerPid/agreement",
            ContractAgreementMessage(
                providerPid = ownPid,
                consumerPid = peerPid,
                // TODO: Properly convert to ODRL agreement before encoding (see processAgree)
                agreement = objectMapper.convertValue<JsonNode>(parsedPolicy),
                callbackAddress = ownEndpoint.toString(),
            ),
        )
        currentPolicy = parsedPolicy
        state = NegotiationState.AGREED
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
        ensurePreconditions(allowedState, Role.CONSUMER, responseCallback)

        val parsedPolicy = OdrlPolicyParser().parse(req.offer)
        val (ok, newReq) = negotiator.evaluateOffer(parsedPolicy)

        if (!ok) {
            if (newReq != null) {
                sendMessage(
                    "$peerPid/request",
                    ContractRequestMessage(
                        providerPid = peerPid,
                        consumerPid = ownPid,
                        callbackAddress = ownEndpoint.toString(),
                        offer = objectMapper.convertValue(newReq),
                    ),
                )
                state = NegotiationState.REQUESTED
                currentPolicy = newReq
                return
            } else {
                terminateNegotiation("errPolicyFailure", "No counteroffer can be computed for this request")
                completed.completeExceptionally(
                    NegotiationException("No counteroffer can be computed for the peer's request"),
                )
                return
            }
        }

        currentPolicy = parsedPolicy
        sendMessage(
            "$peerPid/events",
            ContractNegotiationEventMessage(
                providerPid = peerPid,
                consumerPid = ownPid,
                eventType = NegotiationEvent.ACCEPTED,
            ),
        )
        state = NegotiationState.ACCEPTED
    }

    suspend fun processAccept(responseCallback: suspend (Boolean) -> Unit): Unit =
        mutex.withLock {
            // By making sure that the state machine is in the offered state, we make sure that the current offer has
            // been made by us, as client offers (= requests) move us into the requested state.
            if (!ensurePreconditions(NegotiationState.OFFERED, Role.PROVIDER, responseCallback)) {
                return
            }

            state = NegotiationState.ACCEPTED

            val now = Instant.now()
            val formatted = DateTimeFormatter.ISO_INSTANT.format(now)
            // TODO: We probably need something more to convert to agreement here
            val agreement = objectMapper.convertValue<ObjectNode>(currentPolicy!!)
            // We need to add a DSP-specific timestamp option
            agreement.put("${Namespaces.DSPACE}/timestamp", formatted)
            // HACK: Add a top-level context URL which should translate all unprefixed properties to prefixed ones
            agreement.put("@context", "${Namespaces.ODRL}/")

            sendMessage(
                "$peerPid/agreement",
                ContractAgreementMessage(
                    providerPid = ownPid,
                    consumerPid = peerPid,
                    agreement = agreement,
                    callbackAddress = ownEndpoint.toString(),
                ),
            )
            state = NegotiationState.AGREED
        }

    suspend fun processVerify(responseCallback: suspend (Boolean) -> Unit): Unit =
        mutex.withLock {
            if (!ensurePreconditions(NegotiationState.AGREED, Role.PROVIDER, responseCallback)) {
                return
            }

            state = NegotiationState.VERIFIED

            sendMessage(
                "$peerPid/events",
                ContractNegotiationEventMessage(
                    providerPid = ownPid,
                    consumerPid = peerPid,
                    eventType = NegotiationEvent.FINALIZED,
                ),
            )

            state = NegotiationState.FINALIZED
            completeNegotiation()
        }

    suspend fun processFinalize(responseCallback: suspend (Boolean) -> Unit) {
        if (!ensurePreconditions(NegotiationState.VERIFIED, Role.CONSUMER, responseCallback)) {
            return
        }

        state = NegotiationState.FINALIZED
        completeNegotiation()
    }

    // TODO: Having a locked variant of this that may be invoked by callbacks and could therefore cause a deadlock is
    //       bad. Find out if there are any coroutine reentrant locks in kotlin or if the default mutex can be made
    //       reentrant somehow.
    fun getStateMessageUnlocked(): ContractNegotiation {
        val (providerPid, consumerPid) = getProviderAndConsumerPid()

        return ContractNegotiation(
            providerPid = providerPid,
            consumerPid = consumerPid,
            state = state,
        )
    }

    suspend fun getStateMessage(): ContractNegotiation =
        mutex.withLock {
            return getStateMessageUnlocked()
        }

    suspend fun processAgreement(
        req: ContractAgreementMessage,
        responseCallback: suspend (Boolean) -> Unit,
    ): Unit =
        mutex.withLock {
            if ((state != NegotiationState.REQUESTED && state != NegotiationState.ACCEPTED) || role != Role.CONSUMER) {
                responseCallback(false)
                return
            }

            // TODO: (how) should we evaluate the timestamp? What would be a good timeout to detect outdated requests?
            //       Should this be passed to the negotiator as well?

            val agreement =
                try {
                    OdrlPolicyParser().parse(req.agreement)
                } catch (e: OdrlException) {
                    responseCallback(false)
                    return
                }

            // If the provider tries to send a (semantically) different agreement, reject and terminate the negotiation.
            // We leave the semantics to the negotiator :)
            if (!negotiator.verifyAgreement(currentPolicy!!, agreement)) {
                responseCallback(false)
                terminateNegotiation("errPolicyFailure")
                completed.completeExceptionally(
                    NegotiationException("The policy agreement did not match the offer agreed upon"),
                )
                return
            }

            responseCallback(true)
            state = NegotiationState.VERIFIED
            sendMessage(
                "$peerPid/agreement/verification",
                ContractAgreementVerificationMessage(
                    providerPid = peerPid,
                    consumerPid = ownPid,
                ),
            )
        }

    suspend fun processTermination(
        req: ContractNegotiationTerminationMessage,
        responseCallback: suspend () -> Unit,
    ) = mutex.withLock {
        state = NegotiationState.TERMINATED
        val messageBuilder = StringBuilder("The peer terminated the negotiation with code ${req.code}")
        if (req.reason != null) {
            for (reason in req.reason) {
                messageBuilder.append(": $reason")
            }
        }
        completed.completeExceptionally(NegotiationException(messageBuilder.toString()))
        responseCallback()
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(Negotiation::class.java)
    }

    enum class Role {
        PROVIDER,
        CONSUMER,
    }
}
