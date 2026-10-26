/*
 * Copyright 2024-2026 Fraunhofer AISEC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.fraunhofer.aisec.glue.negotiation.builtin

import com.fasterxml.jackson.databind.ObjectMapper
import de.fraunhofer.aisec.glue.DataStore
import de.fraunhofer.aisec.glue.config.ConfigManager
import de.fraunhofer.aisec.glue.config.ConnectorConfig
import de.fraunhofer.aisec.glue.models.negotiation.ContractNegotiation
import de.fraunhofer.aisec.glue.models.negotiation.ContractRequestMessage
import de.fraunhofer.aisec.glue.models.negotiation.NegotiationState
import de.fraunhofer.aisec.glue.negotiation.Negotiation
import de.fraunhofer.aisec.glue.negotiation.NegotiationManager
import de.fraunhofer.aisec.glue.negotiation.edc.removeEdcPrefix
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
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext
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
    private val dataStore: DataStore,
    private val odrlParser: OdrlParser,
    configManager: ConfigManager,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
) : NegotiationManager() {
    private val connectorConfig = configManager.getConfig(::ConnectorConfig)
    private val negotiations = ConcurrentHashMap<UUID, BuiltinNegotiation>()
    private val pendingConsumerNegotiations = ConcurrentHashMap<UUID, CompletableDeferred<BuiltinNegotiation?>>()
    private val childScope = scope + SupervisorJob() + CoroutineExceptionHandler(this::handleCoroutineException)

    init {
        scope.launch {
            dataStore.negotiations().forEach { data ->
                val negotiation =
                    BuiltinNegotiation(
                        initialData = data,
                        httpClient = httpClient,
                        negotiator = defaultNegotiator,
                        objectMapper = objectMapper,
                        odrlParser = odrlParser,
                        ownIdentity = connectorConfig.identity,
                        scope = childScope,
                    )

                observeNegotiationState(negotiation)
                negotiations[data.id] = negotiation
            }
        }
    }

    /**
     * Start a negotiation by sending the provided contract request to the provider.
     * The callback URL for the negotiation is relative to the `connector.publicUrl` configuration value.
     * The issuer identity used for the offer is set in the `connector.identity` option.
     * @return A future that will yield the negotiated policy.
     */
    override suspend fun startNegotiation(
        peer: String,
        offer: OdrlPolicy,
    ): Negotiation =
        startNegotiation(
            Url(peer),
            connectorConfig.publicUrl,
            offer as? OdrlOffer ?: odrlParser.convertToOdrlOffer(offer, assigners = listOf(OdrlParty(peer))),
        )

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
    ): BuiltinNegotiation {
        val logParams = mutableMapOf<String, Any>()
        val consumerPid = newProcessId()
        pendingConsumerNegotiations[consumerPid] = CompletableDeferred()
        try {
            val contractRequestMessage =
                ContractRequestMessage(
                    id = odrlRequest.uid.removeEdcPrefix(),
                    providerPid = null,
                    consumerPid = consumerPid.toString(),
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
                throw IllegalStateException("Initial HTTP request failed with code ${resp.status}: $body")
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

                    pendingConsumerNegotiations.remove(consumerPid)!!.complete(null)
                    throw IllegalStateException("The provider terminated the negotiation")
                }

                else -> {
                    LOG.atInfo {
                        message = "Peer sent response message indicating an invalid state"
                        payload = logParams
                    }

                    pendingConsumerNegotiations.remove(consumerPid)!!.complete(null)
                    throw IllegalStateException("The provider returned an invalid response")
                }
            }

            // Create the final negotiation object now that we have all the information required.
            // Note that we cannot bind the peer identity yet because we don't have a request with an Authorization header
            // at this point.
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
                    scope = childScope,
                )
            observeNegotiationState(negotiation)

            // This call here is kind of hacky since kotlin coroutines does not have condition variables.
            // In case the peer sends a message to our negotiation object before this function returns, we have it wait.
            // We use this channel to notify any waiting coroutines that this negotiation is usable.
            pendingConsumerNegotiations.remove(consumerPid)!!.complete(negotiation)

            return negotiation
        } catch (e: Exception) {
            // Make sure we don't have coroutines waiting forever.
            pendingConsumerNegotiations.remove(consumerPid)?.cancel("Unhandled exception in start negotiation", e)
            throw e
        }
    }

    /**
     * Create a new contract negotiation representation and write it to the data store.
     */
    suspend fun createNegotiation(
        role: Negotiation.Role,
        peer: String?,
        peerPid: String,
        peerUrl: Url,
        initialState: NegotiationState = NegotiationState.INITIAL,
        callbackUrl: Url = connectorConfig.publicUrl,
        contract: OdrlPolicy? = null,
    ): BuiltinNegotiation {
        val id = newProcessId()
        LOG.atDebug {
            message = "Creating new negotiation object"
            payload = mapOf("id" to id, "role" to role, "peer" to peer, "peerPid" to peerPid, "peerUrl" to peerUrl)
        }

        val negotiation =
            BuiltinNegotiation(
                initialData =
                    BuiltinNegotiationData(
                        id = id,
                        role = role,
                        peer = peer,
                        peerPid = peerPid,
                        startTimestamp = Clock.System.now(),
                        state = initialState,
                        callbackUrl = callbackUrl,
                        peerUrl = peerUrl,
                        contract = contract,
                    ),
                httpClient = httpClient,
                negotiator = defaultNegotiator,
                objectMapper = objectMapper,
                odrlParser = odrlParser,
                ownIdentity = connectorConfig.identity,
                scope = childScope,
            )
        observeNegotiationState(negotiation)
        return negotiation
    }

    /**
     * Get the negotiation for the provided [id], [role], and [peer] identity.
     */
    suspend fun getNegotiation(
        id: String,
        role: Negotiation.Role?,
        peer: String?,
    ): BuiltinNegotiation? {
        // Since all of our IDs are UUIDs, we can safely say that every other format will not be one of our negotiations
        val parsedId = parsePid(id) ?: return null

        val negotiation = negotiations[parsedId] ?: pendingConsumerNegotiations[parsedId]?.await() ?: return null
        if ((role != null && negotiation.role != role) || negotiation.peerIdentity != peer) {
            LOG.atTrace {
                message = "A requested negotiation exists but its role or identity do not match"
                payload =
                    mapOf(
                        "id" to id,
                        "role" to role,
                        "peer" to peer,
                        "expectedRole" to negotiation.role,
                        "expectedPeer" to negotiation.peerIdentity,
                    )
            }
            return null
        }

        return negotiation
    }

    override suspend fun getNegotiations(): Collection<Negotiation> = negotiations.values.toList()

    override suspend fun getNegotiation(id: String): Negotiation? = parsePid(id)?.let(negotiations::get)

    private fun parsePid(providerPid: String): UUID? {
        val urnPattern = Regex("(?:urn:uuid:)?([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})")
        return urnPattern
            .find(providerPid)
            ?.groupValues
            ?.get(1)
            ?.let { UUID.fromString(it) }
    }

    private fun newProcessId(): UUID = UUID.randomUUID()

    /**
     * Observe updates to the negotiation state and updates and dispatch them accordingly.
     * Optionally accepts a future to receive the final contract when finishing the negotiation.
     */
    private suspend fun observeNegotiationState(negotiation: BuiltinNegotiation) {
        dataStore.addNegotiation(negotiation.data)
        negotiation.stateObservers.add { data ->
            LOG.atTrace {
                message = "Observing negotiation state update"
                payload = mapOf("negotiation" to data)
            }

            dataStore.updateNegotiation(data)

            if (data.state == NegotiationState.FINALIZED) {
                assert(data.contract != null && data.contract is OdrlAgreement)
                dataStore.addContract(data.contract as OdrlAgreement)
            }
        }
        negotiations[negotiation.data.id] = negotiation
    }

    private fun handleCoroutineException(
        context: CoroutineContext,
        exception: Throwable,
    ) {
        val negotiation = context[NegotiationContext]?.negotiation
        val name = context[CoroutineName]?.name ?: "<unknown>"
        LOG.atError {
            message = "Unhandled exception in negotiation coroutine"
            cause = exception
            payload = mapOf("negotiation" to negotiation?.data, "coroutine" to name)
        }

        if (negotiation != null) {
            // Cancellation is async as we try to send a message to the peer.
            // The name is set here to make debugging easier for nested failures.
            childScope.launch(CoroutineName("$name-cleanup")) {
                negotiation.cancel("Unhandled exception in negotiation coroutine")
            }
        }
    }

    companion object {
        private val LOG = KotlinLogging.logger {}
    }
}
