package de.fraunhofer.aisec.glue.negotiation.builtin

import com.fasterxml.jackson.databind.ObjectMapper
import de.fraunhofer.aisec.glue.ContractStore
import de.fraunhofer.aisec.glue.config.ConfigManager
import de.fraunhofer.aisec.glue.config.ConnectorConfig
import de.fraunhofer.aisec.glue.models.negotiation.ContractNegotiation
import de.fraunhofer.aisec.glue.models.negotiation.ContractOfferMessage
import de.fraunhofer.aisec.glue.models.negotiation.ContractRequestMessage
import de.fraunhofer.aisec.glue.models.negotiation.NegotiationState
import de.fraunhofer.aisec.glue.negotiation.Negotiation
import de.fraunhofer.aisec.glue.negotiation.NegotiationCompletionCallback
import de.fraunhofer.aisec.glue.negotiation.NegotiationManager
import de.fraunhofer.iosb.odrl.OdrlAgreement
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import java.util.Collections.synchronizedSet
import java.util.concurrent.ConcurrentHashMap

/**
 * The negotiation manager to use to enable builtin negotiation.
 * This class should be used in conjunction with the [NegotiationController] class, which provides the HTTP endpoints
 * required to perform negotiations.
 */
class BuiltinNegotiationManager(
    private val httpClient: HttpClient,
    private val objectMapper: ObjectMapper,
    private val defaultNegotiator: Negotiator,
    private val contractStore: ContractStore,
    private val odrlParser: OdrlParser,
    private val configManager: ConfigManager,
) : NegotiationManager() {
    private val pidMutex = Mutex()
    private val assignedIds = synchronizedSet(mutableSetOf<String>())
    private val providerNegotiations = ConcurrentHashMap<String, BuiltinNegotiation>()
    private val consumerNegotiations = ConcurrentHashMap<String, BuiltinNegotiation>()
    private val pendingConsumerNegotiations = ConcurrentHashMap<String, MutableSharedFlow<BuiltinNegotiation?>>()
    private val connectorConfig = configManager.getConfig(::ConnectorConfig)

    /**
     * Start a negotiation by sending the provided contract request to the provider.
     * The callback URL for the negotiation is relative to the `connector.publicUrl` configuration value.
     * The issuer identity used for the offer is set in the `connector.identity` option.
     * @return A future that will yield the negotiated policy.
     */
    override suspend fun startNegotiation(
        peer: String,
        offer: OdrlPolicy,
    ): Deferred<OdrlAgreement> {
        return startNegotiation(
            Url(peer),
            URLBuilder(connectorConfig.publicUrl).appendPathSegments("negotiations").build(),
            odrlParser.convertToOdrlOffer(offer, assigners = listOf(OdrlParty(connectorConfig.identity))),
        )
    }

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
    ): Deferred<OdrlAgreement> {
        val logParams = mutableMapOf<String, Any>()
        val consumerPid = newProcessId()
        pendingConsumerNegotiations[consumerPid] = MutableSharedFlow()

        val contractRequestMessage =
            ContractRequestMessage(
                providerPid = null,
                consumerPid = consumerPid,
                callbackUrl.toString(),
                odrlRequest,
            )

        logParams["consumerPid"] = consumerPid
        logParams["peerUrl"] = provider
        logParams["callbackUrl"] = callbackUrl
        logParams["contractOffer"] = odrlRequest.uid
        LOG.atDebug {
            message = "Creating new negotiation object"
            payload = logParams
        }

        val requestUrl = URLBuilder(provider).appendPathSegments("request").build()

        val resp =
            httpClient.post(requestUrl) {
                contentType(ContentType.Application.Json)
                setBody(contractRequestMessage)
                // Authentication will be handled on (A)TLS layer
            }

        if (!resp.status.isSuccess()) {
            val body = resp.bodyAsText()
            LOG.atInfo {
                message = "Starting negotiation failed due to peer returning an error"
                payload =
                    buildMap {
                        putAll(logParams)
                        put("status", resp.status)
                        put("description", body)
                    }
            }
            return CompletableDeferred<OdrlAgreement>().apply {
                cancel("Initial HTTP request failed with code ${resp.status}: $body")
            }
        }

        val deserializedResponse = resp.body<ContractNegotiation>()

        logParams["providerPid"] = deserializedResponse.providerPid
        LOG.atDebug {
            message = "Initial negotiation setup successful"
            payload =
                buildMap {
                    putAll(logParams)
                    put("state", deserializedResponse.state)
                }
        }

        when (deserializedResponse.state) {
            // The negotiation has started, should get a contract offer soon
            NegotiationState.REQUESTED -> {}
            // The provider terminated the negotiation
            NegotiationState.TERMINATED -> {
                LOG.atInfo {
                    message = "Peer terminated the negotiation after the initial request"
                    payload = logParams
                }

                assignedIds.remove(consumerPid)
                pendingConsumerNegotiations.remove(consumerPid)!!.tryEmit(null)
                return CompletableDeferred<OdrlAgreement>().apply {
                    cancel("The provider terminated the negotiation")
                }
            }

            else -> {
                LOG.atInfo {
                    message = "Peer sent response message indicating an invalid state"
                    payload = logParams
                }

                assignedIds.remove(consumerPid)
                pendingConsumerNegotiations.remove(consumerPid)!!.tryEmit(null)
                return CompletableDeferred<OdrlAgreement>().apply {
                    cancel("The provider returned an invalid response")
                }
            }
        }

        // We return a future object that we use to notify code of when a negotiation is finished.
        // Note that this is not good enough for when negotiation takes really long.
        val policyFuture = CompletableDeferred<OdrlAgreement>()

        // Create the final negotiation object now that we have all the information required.
        // Note that we cannot bind the peer identity yet because we don't have a request with an Authorization header
        // at this point.
        // TODO: When we define the authentication model eventually, revisit this
        val negotiation =
            BuiltinNegotiation(
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
                        policyFuture.completeWith(result.map { it as OdrlAgreement })
                        consumerNegotiations.remove(consumerPid)
                    },
                odrlParser = odrlParser,
            )

        // This call here is kind of hacky since kotlin coroutines does not have condition variables.
        // In case the peer sends a message to our negotiation object before this function returns, we have it wait.
        // We use this channel to notify any waiting coroutines that this negotiation is usable.
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
    ): BuiltinNegotiation? {
        val logParams = mutableMapOf("consumerPid" to request.consumerPid, "peerIdentity" to peerIdent)

        // We call this function when we receive a new contract request message, which can also happen for a follow-up
        // request in case we reject the initial contract.
        // In that special case, the negotiation already exists and we use the getter instead.
        if (request.providerPid != null) {
            LOG.atDebug {
                message = "This negotiation already exists, switching to getter"
                payload =
                    buildMap {
                        putAll(logParams)
                        put("providerPid", request.providerPid)
                    }
            }
            return getAsProvider(request.providerPid, request.consumerPid, peerIdent)
        }

        val providerPid = newProcessId()

        logParams["providerPid"] = providerPid
        LOG.atDebug {
            message = "Creating new negotiation object as provider for request"
            payload = logParams
        }

        val negotiation =
            BuiltinNegotiation(
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
                odrlParser = odrlParser,
            )

        providerNegotiations[providerPid] = negotiation
        return negotiation
    }

    suspend fun createFromOffer(
        request: ContractOfferMessage,
        originUrl: Url,
        peerIdent: String,
    ): BuiltinNegotiation? {
        val logParams = mutableMapOf("providerPid" to request.providerPid, "peerIdentity" to peerIdent)

        if (request.consumerPid != null) {
            LOG.atDebug {
                message = "This negotiation already exists, switching to getter"
                payload =
                    buildMap {
                        putAll(logParams)
                        put("consumerPid", request.consumerPid)
                    }
            }
            return getAsConsumer(request.consumerPid, request.providerPid, peerIdent)
        }

        val consumerPid = newProcessId()

        logParams["consumerPid"] = consumerPid
        LOG.atDebug {
            message = "Creating new negotiation object for offer as consumer"
            payload = logParams
        }

        val negotiation =
            BuiltinNegotiation(
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
                odrlParser = odrlParser,
            )

        consumerNegotiations[consumerPid] = negotiation
        return negotiation
    }

    fun getAsProvider(
        providerPid: String,
        consumerPid: String?,
        peerIdent: String,
    ): BuiltinNegotiation? {
        val logParams = mutableMapOf<String, Any>()
        val negotiation = providerNegotiations[providerPid] ?: return null

        logParams["providerPid"] to providerPid
        logParams["consumerPid"] = consumerPid ?: "unset"
        logParams["peerIdentity"] = peerIdent

        if (consumerPid != null && negotiation.peerPid != consumerPid) {
            LOG.atDebug {
                message = "Cannot get negotiation as provider as the consumer pid does not match"
                payload =
                    buildMap {
                        putAll(logParams)
                        put("expectedConsumerPid", negotiation.peerPid)
                    }
            }
            // While this would be more of a bad request scenario, but we just return not found in order to prevent
            // even the remote possibility of probing for negotiations
            return null
        }

        // We check if the identity of the negotiation partner is set correctly
        if (negotiation.peerIdentity != null && negotiation.peerIdentity != peerIdent) {
            LOG.atDebug {
                message = "Cannot get negotiation as provider as the peer identity does not match"
                payload =
                    buildMap {
                        putAll(logParams)
                        put("expectedPeerIdentity", negotiation.peerIdentity)
                    }
            }
            return null
        }

        return negotiation
    }

    suspend fun getAsConsumer(
        consumerPid: String,
        providerPid: String?,
        peerIdent: String,
    ): BuiltinNegotiation? {
        val logParams =
            mutableMapOf<String, Any>("consumerPid" to consumerPid, "providerPid" to (providerPid ?: "unset"), "peerIdentity" to peerIdent)

        // We have to be careful when getting the negotiation object as a consumer since we are the initiator here.
        // When the peer is implemented in a way that it sends out callbacks before we're ready the negotiation object
        // may not be in the consumerNegotiations map yet.
        // To avoid this, we use a channel that this function can block on.
        // Note that this is not perfect since there is a (theoretical) race condition here when the initial negotiation
        // resolves at the same time we try to get the channel here.
        // This is incredibly unlikely, but may cause some requests to fail.
        val negotiationProducer =
            if (consumerNegotiations.containsKey(consumerPid)) {
                val negotiation = consumerNegotiations[consumerPid]!!
                LOG.atDebug {
                    message = "Using existing negotiation object"
                    payload = logParams
                }
                suspend { negotiation }
            } else if (pendingConsumerNegotiations.containsKey(consumerPid)) {
                val channel = pendingConsumerNegotiations[consumerPid]!!
                LOG.atDebug {
                    message = "Waiting for pending negotiation object"
                    payload = logParams
                }
                suspend { channel.first() }
            } else {
                suspend { null }
            }
        val negotiation = negotiationProducer() ?: return null

        if (providerPid != null && negotiation.peerPid != providerPid) {
            LOG.atDebug {
                message = "Cannot get negotiation as consumer as the provider pid does not match"
                payload =
                    buildMap {
                        putAll(logParams)
                        put("expectedProviderPid", negotiation.peerPid)
                    }
            }
            return null
        }

        if (negotiation.peerIdentity != null && negotiation.peerIdentity != peerIdent) {
            LOG.atDebug {
                message = "Cannot get negotiation as consumer as the peer identity does not match"
                payload =
                    buildMap {
                        putAll(logParams)
                        put("expectedPeerIdentity", negotiation.peerIdentity)
                    }
            }
            return null
        }

        return negotiation
    }

    override suspend fun getNegotiations(): Collection<BuiltinNegotiation> {
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
                onFailure = { err -> LOG.info(err) { "Policy negotiation failed" } },
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
        private val LOG = KotlinLogging.logger {}
    }
}
