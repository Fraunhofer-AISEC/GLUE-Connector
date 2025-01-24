package de.fraunhofer.aisec.glue.negotiation

import com.fasterxml.jackson.databind.ObjectMapper
import de.fraunhofer.aisec.glue.ContractStore
import de.fraunhofer.aisec.glue.models.negotiation.ContractNegotiation
import de.fraunhofer.aisec.glue.models.negotiation.ContractOfferMessage
import de.fraunhofer.aisec.glue.models.negotiation.ContractRequestMessage
import de.fraunhofer.aisec.glue.models.negotiation.NegotiationState
import de.fraunhofer.iosb.odrl.OdrlOffer
import de.fraunhofer.iosb.odrl.OdrlPolicy
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import java.util.*
import java.util.Collections.synchronizedSet
import java.util.concurrent.ConcurrentHashMap

class NegotiationManager(
    private val httpClient: HttpClient,
    private val objectMapper: ObjectMapper,
    private val defaultNegotiator: Negotiator,
    private val contractStore: ContractStore,
) {
    private val pidMutex = Mutex()
    private val assignedIds = synchronizedSet(mutableSetOf<String>())
    private val providerNegotiations = ConcurrentHashMap<String, NegotiationImpl>()
    private val consumerNegotiations = ConcurrentHashMap<String, NegotiationImpl>()
    private val pendingConsumerNegotiations = ConcurrentHashMap<String, MutableSharedFlow<NegotiationImpl?>>()

    /**
     * Start a negotiation by sending the provided contract request to the provider.
     * A custom negotiator can be used to evaluate any offer returned by the peer.
     * Return a future that will yield a policy object.
     */
    suspend fun startNegotiation(
        provider: Url,
        callbackUrl: Url,
        odrlRequest: OdrlOffer,
        negotiator: Negotiator? = null,
    ): Deferred<OdrlPolicy> {
        val consumerPid = newProcessId()
        pendingConsumerNegotiations[consumerPid] = MutableSharedFlow()

        // TODO: We probably need a custom serializer here since the ODRL classes don't have the jackson tags for
        //       automatic jsonld serialization.
        //       Maybe we can get away with setting the base to the ODRL namespace.
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
                assignedIds.remove(consumerPid)
                pendingConsumerNegotiations.remove(consumerPid)!!.tryEmit(null)
                return CompletableDeferred<OdrlPolicy>().apply {
                    cancel("The provider terminated the negotiation")
                }
            }

            else -> {
                assignedIds.remove(consumerPid)
                pendingConsumerNegotiations.remove(consumerPid)!!.tryEmit(null)
                return CompletableDeferred<OdrlPolicy>().apply {
                    cancel("The provider returned an invalid response")
                }
            }
        }

        val policyFuture = CompletableDeferred<OdrlPolicy>()

        // Create the final negotiation object now that we have all the information required.
        // Note that we cannot bind the peer identity yet because we don't have a request with an Authorization header
        // at this point.
        // TODO: When we define the authentication model eventually, revisit this
        val negotiation =
            NegotiationImpl(
                state = NegotiationState.REQUESTED,
                peerIdentity = null,
                currentPolicy = odrlRequest,
                role = Negotiation.Role.CONSUMER,
                ownPid = consumerPid,
                peerPid = deserializedResponse.providerPid,
                negotiator = negotiator ?: defaultNegotiator,
                httpClient = httpClient,
                ownEndpoint = callbackUrl,
                peerEndpoint = provider,
                objectMapper = objectMapper,
                completionCallback =
                    completionCallback { result ->
                        policyFuture.completeWith(result)
                        consumerNegotiations.remove(consumerPid)
                    },
            )

        pendingConsumerNegotiations.remove(consumerPid)!!.tryEmit(negotiation)
        consumerNegotiations[consumerPid] = negotiation

        return policyFuture
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

        val providerPid = newProcessId()

        val negotiation =
            NegotiationImpl(
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
                completionCallback =
                    completionCallback {
                        providerNegotiations.remove(providerPid)
                    },
            )

        providerNegotiations[providerPid] = negotiation
        return negotiation
    }

    suspend fun createFromOffer(
        request: ContractOfferMessage,
        originUrl: Url,
        peerIdent: String,
    ): Negotiation? {
        if (request.consumerPid != null) {
            return getAsConsumer(request.consumerPid, request.providerPid, peerIdent)
        }

        val consumerPid = newProcessId()

        val negotiation =
            NegotiationImpl(
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
                completionCallback =
                    completionCallback {
                        pidMutex.withLock {
                            consumerNegotiations.remove(consumerPid)
                        }
                    },
            )

        consumerNegotiations[consumerPid] = negotiation
        return negotiation
    }

    fun getAsProvider(
        providerPid: String,
        consumerPid: String?,
        peerIdent: String,
    ): Negotiation? {
        val negotiation = providerNegotiations[providerPid] ?: return null

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
        val negotiationProducer =
            if (consumerNegotiations.containsKey(consumerPid)) {
                val negotiation = consumerNegotiations[consumerPid]!!
                suspend { negotiation }
            } else if (pendingConsumerNegotiations.containsKey(consumerPid)) {
                val channel = pendingConsumerNegotiations[consumerPid]!!
                suspend { channel.first() }
            } else {
                suspend { null }
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

    fun getNegotiations(): List<Negotiation> {
        return (consumerNegotiations.values + providerNegotiations.values).toList()
    }

    private suspend fun newProcessId(): String {
        pidMutex.withLock {
            return generateSequence { UUID.randomUUID() }
                .map { uuid -> "urn:uuid:$uuid" }
                .first { id -> !assignedIds.contains(id) }
                .also { assignedIds.add(it) }
        }
    }

    // Perform actions required by all our registered negotiations after completion and then call a negotiation-specific
    // completion callback.
    private fun completionCallback(next: NegotiationCompletionCallback): NegotiationCompletionCallback {
        return { result: Result<OdrlPolicy> ->
            result.fold(
                onSuccess = { contract -> contractStore.addContract(contract) },
                onFailure = { err -> LOG.info("Policy negotiation failed", err) },
            )
            // We call the next callback asynchronously since it may need to acquire locks that are held up the call
            // stack.
            // A reentrant lock may solve this locking problem more elegantly, but I'm not aware of one for kotlin
            // coroutines.
            CoroutineScope(Dispatchers.Default).launch {
                next(result)
            }
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(NegotiationManager::class.java)
    }
}
