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
import de.fraunhofer.aisec.glue.negotiation.NegotiationException
import de.fraunhofer.aisec.glue.negotiation.NegotiationManager
import de.fraunhofer.aisec.glue.negotiation.edc.toEdcFormat
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import java.util.Collections.synchronizedSet
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Clock

/**
 * The negotiation manager to use to enable builtin negotiation.
 * This class should be used in conjunction with the [NegotiationController] class, which provides the HTTP endpoints
 * required to perform negotiations.
 */
open class BuiltinNegotiationManager(
    private val httpClient: HttpClient,
    private val objectMapper: ObjectMapper,
    private val defaultNegotiator: Negotiator,
    private val contractStore: ContractStore,
    private val odrlParser: OdrlParser,
    configManager: ConfigManager,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
) : NegotiationManager() {
    private val connectorConfig = configManager.getConfig(::ConnectorConfig)
    private val pidMutex = Mutex()
    private val assignedIds = synchronizedSet(mutableSetOf<String>())
    private val negotiations = ConcurrentHashMap<String, BuiltinNegotiation>()
    private val pendingConsumerNegotiations = ConcurrentHashMap<String, CompletableDeferred<BuiltinNegotiation?>>()

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
            connectorConfig.publicUrl,
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
        pendingConsumerNegotiations[consumerPid] = CompletableDeferred()
        try {
            val contractRequestMessage =
                ContractRequestMessage(
                    uid = odrlRequest.uid.toEdcFormat(),
                    providerPid = null,
                    consumerPid = consumerPid,
                    callbackAddress = callbackUrl.toString(),
                    offer = odrlRequest,
                )

            logParams["id"] = consumerPid
            logParams["peerUrl"] = provider
            logParams["callbackUrl"] = callbackUrl
            logParams["contractOffer"] = odrlRequest.uid
            LOG.atDebug {
                message = "Creating new negotiation object"
                payload = logParams
            }

            val requestUrl = URLBuilder(provider).appendPathSegments("negotiations", "request").build()

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
                    pendingConsumerNegotiations.remove(consumerPid)!!.complete(null)
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
                    pendingConsumerNegotiations.remove(consumerPid)!!.complete(null)
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
            // TODO:
            val negotiation =
                BuiltinNegotiation(
                    initialData =
                        BuiltinNegotiationData(
                            id = consumerPid,
                            role = Negotiation.Role.CONSUMER,
                            peer = null,
                            peerPid = deserializedResponse.providerPid,
                            startTimestamp = Clock.System.now(),
                            state = NegotiationState.REQUESTED,
                            callbackUrl = callbackUrl,
                            peerUrl = provider,
                            contract = odrlRequest,
                        ),
                    httpClient = httpClient,
                    negotiator = negotiator ?: defaultNegotiator,
                    objectMapper = objectMapper,
                    odrlParser = odrlParser,
                    ownIdentity = connectorConfig.identity,
                    scope = scope,
                )
            observeNegotiationState(negotiation, policyFuture)
            negotiations[consumerPid] = negotiation

            LOG.atTrace { message = "About to emit negotiation for consumer $consumerPid" }

            // This call here is kind of hacky since kotlin coroutines does not have condition variables.
            // In case the peer sends a message to our negotiation object before this function returns, we have it wait.
            // We use this channel to notify any waiting coroutines that this negotiation is usable.
            pendingConsumerNegotiations.remove(consumerPid)!!.complete(negotiation)

            return policyFuture
        } catch (e: Exception) {
            // Make sure we don't have coroutines waiting forever.
            pendingConsumerNegotiations.remove(consumerPid)?.cancel("Unhandled exception in start negotiation", e)
            throw e
        }
    }

    /**
     * Create a new negotiation object from a consumer's request.
     * If the request contains a PID for an existing negotiation, this function is equivalent to [getAsProvider].
     */
    suspend fun createFromRequest(
        request: ContractRequestMessage,
        peerIdent: String?,
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

        logParams["id"] = providerPid
        LOG.atDebug {
            message = "Creating new negotiation object as provider for request"
            payload = logParams
        }

        val negotiation =
            BuiltinNegotiation(
                initialData =
                    BuiltinNegotiationData(
                        id = providerPid,
                        role = Negotiation.Role.PROVIDER,
                        peer = peerIdent,
                        peerPid = request.consumerPid,
                        startTimestamp = Clock.System.now(),
                        state = NegotiationState.INITIAL,
                        callbackUrl = connectorConfig.publicUrl,
                        peerUrl = Url(request.callbackAddress),
                    ),
                httpClient = httpClient,
                negotiator = defaultNegotiator,
                objectMapper = objectMapper,
                odrlParser = odrlParser,
                ownIdentity = connectorConfig.identity,
                scope = scope,
            )

        observeNegotiationState(negotiation, null)
        negotiations[providerPid] = negotiation
        return negotiation
    }

    suspend fun createFromOffer(
        request: ContractOfferMessage,
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

        logParams["id"] = consumerPid
        LOG.atDebug {
            message = "Creating new negotiation object for offer as consumer"
            payload = logParams
        }

        val negotiation =
            BuiltinNegotiation(
                initialData =
                    BuiltinNegotiationData(
                        id = consumerPid,
                        role = Negotiation.Role.CONSUMER,
                        peer = peerIdent,
                        peerPid = request.providerPid,
                        startTimestamp = Clock.System.now(),
                        state = NegotiationState.INITIAL,
                        callbackUrl = connectorConfig.publicUrl,
                        peerUrl = Url(request.callbackAddress),
                    ),
                httpClient = httpClient,
                negotiator = defaultNegotiator,
                objectMapper = objectMapper,
                odrlParser = odrlParser,
                ownIdentity = connectorConfig.identity,
                scope = scope,
            )
        observeNegotiationState(negotiation, null)
        negotiations[consumerPid] = negotiation
        return negotiation
    }

    fun getAsProvider(
        providerPid: String,
        consumerPid: String?,
        peerIdent: String?,
    ): BuiltinNegotiation? {
        val logParams = mutableMapOf<String, Any?>()
        val negotiation = negotiations[providerPid] ?: return null
        if (negotiation.role != Negotiation.Role.PROVIDER) {
            // The ID is used, but not for a provider negotiation.
            return null
        }

        logParams["providerPid"] = providerPid
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
        if (negotiation.peerIdentity != peerIdent) {
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
        peerIdent: String?,
    ): BuiltinNegotiation? {
        val logParams =
            mutableMapOf("consumerPid" to consumerPid, "providerPid" to (providerPid ?: "unset"), "peerIdentity" to peerIdent)

        val negotiation = negotiations[consumerPid] ?: pendingConsumerNegotiations[consumerPid]?.await() ?: return null
        if (negotiation.role != Negotiation.Role.CONSUMER) {
            // The ID is used, but not for a consumer negotiation.
            return null
        }

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

        if (negotiation.peerIdentity != peerIdent) {
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

    override suspend fun getNegotiations(): Collection<Negotiation> {
        return negotiations.values.toList()
    }

    private suspend fun newProcessId(): String {
        pidMutex.withLock {
            return generateSequence { UUID.randomUUID() }
                .map { uuid -> "urn:uuid:$uuid" }
                .first { id -> !assignedIds.contains(id) }
                .also { assignedIds.add(it) }
        }
    }

    /**
     * Observe updates to the negotiation state and updates and dispatch them accordingly.
     * Optionally accepts a future to receive the final contract when finishing the negotiation.
     */
    private fun observeNegotiationState(
        negotiation: BuiltinNegotiation,
        future: CompletableDeferred<OdrlAgreement>?,
    ) {
        scope.launch {
            negotiation.data.collect { data ->
                // TODO: Update the state of the negotiation object in the data store
                // e.g. negotiationsStore.set(update)

                if (data.state == NegotiationState.FINALIZED || data.state == NegotiationState.TERMINATED) {
                    val removed = negotiations.remove(data.id)
                    assert(negotiation == removed)

                    if (data.state == NegotiationState.FINALIZED) {
                        assert(data.contract != null && data.contract is OdrlAgreement)

                        contractStore.addContract(data.contract as OdrlAgreement)
                        future?.complete(data.contract)
                    } else {
                        future?.completeExceptionally(NegotiationException(data.failureReason ?: "Unknown Error"))
                    }
                }
            }
        }
    }

    companion object {
        private val LOG = KotlinLogging.logger {}
    }
}
