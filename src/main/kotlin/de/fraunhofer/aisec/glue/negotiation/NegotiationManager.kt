package de.fraunhofer.aisec.glue.negotiation

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import de.fraunhofer.aisec.glue.models.negotiation.ContractNegotiation
import de.fraunhofer.aisec.glue.models.negotiation.ContractOfferMessage
import de.fraunhofer.aisec.glue.models.negotiation.ContractRequestMessage
import de.fraunhofer.aisec.glue.models.negotiation.NegotiationState
import fhg.glue.odrl.OdrlPolicy
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class NegotiationManager(
    private val httpClient: HttpClient,
    private val objectMapper: ObjectMapper,
    private val defaultNegotiator: Negotiator,
) {
    private val pidMutex = Mutex()
    private val assignedIds = mutableSetOf<String>()
    private val providerNegotiation = ConcurrentHashMap<String, Negotiation>()
    private val consumerNegotiations = ConcurrentHashMap<String, Negotiation>()
    private val pendingConsumerNegotiations = ConcurrentHashMap<String, Channel<Negotiation?>>()

    private val newNegotiationsFlow = MutableSharedFlow<Deferred<OdrlPolicy>>()
    val newNegotiations = newNegotiationsFlow.asSharedFlow()

    /**
     * Start a negotiation by sending the provided contract request to the provider.
     * A custom negotiator can be used to evaluate any offer returned by the peer.
     * Return a future that will yield a policy object.
     */
    suspend fun startNegotiation(
        provider: Url,
        callbackUrl: Url,
        // TODO: Perhaps accept the parsed policy if we get serialization to work
        request: JsonNode,
        negotiator: Negotiator? = null,
    ): Deferred<OdrlPolicy> {
        val consumerPid =
            pidMutex.withLock {
                val consumerPid = newProcessId()
                assignedIds.add(consumerPid)
                pendingConsumerNegotiations[consumerPid] = Channel()
                consumerPid
            }

        // TODO: We probably need a custom serializer here since the ODRL classes don't have the jackson tags for
        //       automatic jsonld serialization.
        //       Maybe we can get away with setting the base to the ODRL namespace.
        val odrlRequest = objectMapper.convertValue<JsonNode>(request)
        val contractRequestMessage =
            ContractRequestMessage(
                providerPid = null,
                consumerPid = consumerPid,
                callbackUrl.toString(),
                odrlRequest,
            )

        val requestUrl = URLBuilder(provider).appendPathSegments("request").build()

        val resp =
            httpClient.post(requestUrl) {
                contentType(ContentType.Application.Json)
                setBody(contractRequestMessage)
                // Authentication should be handled by the HTTP client via plugin
            }

        if (!resp.status.isSuccess()) {
            return CompletableDeferred<OdrlPolicy>().apply {
                cancel("Initial HTTP request failed with code ${resp.status}: ${resp.bodyAsText()}")
            }
        }

        val deserializedResponse = resp.body<ContractNegotiation>()

        when (deserializedResponse.state) {
            // The negotiation has started, should get a contract offer soon
            NegotiationState.REQUESTED -> {}
            // The provider terminated the negotiation
            NegotiationState.TERMINATED -> {
                pidMutex.withLock {
                    assignedIds.remove(consumerPid)
                    pendingConsumerNegotiations.remove(consumerPid)!!.run {
                        trySend(null)
                        close()
                    }
                }
                return CompletableDeferred<OdrlPolicy>().apply {
                    cancel("The provider terminated the negotiation")
                }
            }
            else -> {
                pidMutex.withLock {
                    assignedIds.remove(consumerPid)
                    pendingConsumerNegotiations.remove(consumerPid)!!.run {
                        trySend(null)
                        close()
                    }
                }
                return CompletableDeferred<OdrlPolicy>().apply {
                    cancel("The provider returned an invalid response")
                }
            }
        }

        // Create the final negotiation object now that we have all the information required.
        // Note that we cannot bind the peer identity yet because we don't have a request with an Authorization header
        // at this point.
        // TODO: When we define the authentication model eventually, revisit this
        val negotiation =
            Negotiation(
                state = NegotiationState.REQUESTED,
                peerIdentity = null,
                currentPolicy = request,
                role = Negotiation.Role.CONSUMER,
                ownPid = consumerPid,
                peerPid = deserializedResponse.providerPid,
                negotiator = negotiator ?: defaultNegotiator,
                httpClient = httpClient,
                ownEndpoint = callbackUrl,
                peerEndpoint = provider,
                objectMapper = objectMapper,
            )

        pidMutex.withLock {
            pendingConsumerNegotiations.remove(consumerPid)!!.trySend(negotiation)
            consumerNegotiations[consumerPid] = negotiation
        }

        // TODO: Do we need the negotiation object to survive beyond the termination of the negotiation?
        //       If not, register a completion handler here that deletes the negotiation object.

        newNegotiationsFlow.emit(negotiation.completed)
        return negotiation.completed
    }

    /**
     * Create a new negotiation object from a consumer's request.
     * If the request contains a PID for an existing negotiation, this function is equivalent to [getAsProvider].
     */
    suspend fun createFromRequest(
        request: ContractRequestMessage,
        originUrl: Url,
        peerIdent: String,
    ): Negotiation? {
        if (request.providerPid != null) {
            return getAsProvider(request.providerPid, request.consumerPid, peerIdent)
        }

        pidMutex.withLock {
            val providerPid = newProcessId()
            assignedIds.add(providerPid)

            val negotiation =
                Negotiation(
                    state = NegotiationState.REQUESTED,
                    peerIdentity = peerIdent,
                    // The policy is actually set after it has been processed by the negotiator
                    currentPolicy = null,
                    role = Negotiation.Role.PROVIDER,
                    ownPid = providerPid,
                    peerPid = request.consumerPid,
                    negotiator = defaultNegotiator,
                    httpClient = httpClient,
                    ownEndpoint = originUrl,
                    peerEndpoint = Url(request.callbackAddress),
                    objectMapper = objectMapper,
                )
            providerNegotiation[providerPid] = negotiation
            newNegotiationsFlow.emit(negotiation.completed)
            return negotiation
        }
    }

    suspend fun createFromOffer(
        request: ContractOfferMessage,
        originUrl: Url,
        peerIdent: String,
    ): Negotiation? {
        if (request.consumerPid != null) {
            return getAsConsumer(request.consumerPid, request.providerPid, peerIdent)
        }

        pidMutex.withLock {
            val consumerPid = newProcessId()
            assignedIds.add(consumerPid)

            val negotiation =
                Negotiation(
                    state = NegotiationState.OFFERED,
                    peerIdentity = peerIdent,
                    currentPolicy = null,
                    role = Negotiation.Role.CONSUMER,
                    ownPid = consumerPid,
                    peerPid = request.providerPid,
                    negotiator = defaultNegotiator,
                    httpClient = httpClient,
                    ownEndpoint = originUrl,
                    peerEndpoint = Url(request.callbackAddress),
                    objectMapper = objectMapper,
                )

            consumerNegotiations[consumerPid] = negotiation
            newNegotiationsFlow.emit(negotiation.completed)
            return negotiation
        }
    }

    fun getAsProvider(
        providerPid: String,
        consumerPid: String?,
        peerIdent: String,
    ): Negotiation? {
        val negotiation = providerNegotiation[providerPid] ?: return null

        if (consumerPid != null && negotiation.peerPid != consumerPid) {
            // While this would be more of a bad request scenario, but we just return not found in order to prevent
            // even the remote possibility of probing for negotiations
            return null
        }

        if (negotiation.peerIdentity != null && negotiation.peerIdentity != peerIdent) {
            return null
        }

        return negotiation
    }

    suspend fun getAsConsumer(
        consumerPid: String,
        providerPid: String?,
        peerIdent: String,
    ): Negotiation? {
        // This is needed since kotlinx coroutines does not support condition variables...
        // TODO: If this function is called multiple times while the negotiation is still pending, only one of the calls
        //       will be successful. During normal operation, this is no problem, right?
        val negotiationProducer =
            pidMutex.withLock {
                if (consumerNegotiations.containsKey(consumerPid)) {
                    val negotiation = consumerNegotiations[consumerPid]!!
                    suspend { negotiation }
                } else if (pendingConsumerNegotiations.containsKey(consumerPid)) {
                    val channel = pendingConsumerNegotiations[consumerPid]!!
                    suspend { channel.receiveCatching().getOrNull() }
                } else {
                    suspend { null }
                }
            }
        val negotiation = negotiationProducer() ?: return null

        if (providerPid != null && negotiation.peerPid != providerPid) {
            return null
        }

        if (negotiation.peerIdentity != null && negotiation.peerIdentity != peerIdent) {
            return null
        }

        return negotiation
    }

    private fun newProcessId(): String {
        return generateSequence { UUID.randomUUID() }
            .map { uuid -> "urn:uuid:$uuid" }
            .first { id -> !assignedIds.contains(id) }
    }
}
