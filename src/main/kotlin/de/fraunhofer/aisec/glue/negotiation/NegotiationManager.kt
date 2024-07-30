package de.fraunhofer.aisec.glue.negotiation

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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
import io.ktor.util.collections.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class NegotiationManager(private val httpClient: HttpClient, private val defaultNegotiator: Negotiator) {
    private val providerNegotiation = ConcurrentHashMap<String, Negotiation>()
    private val providerPidMutex = Mutex()

    private val consumerNegotiations = ConcurrentHashMap<String, Negotiation>()
    private val consumerReservedIds = ConcurrentSet<String>()
    private val consumerPidMutex = Mutex()

    /**
     * Start a negotiation by sending the provided contract request to the provider.
     * A custom negotiator can be used to evaluate any offer returned by the peer.
     * Return a future that will yield a policy object.
     */
    suspend fun startNegotiation(provider: Url, callbackUrl: Url, request: OdrlPolicy, negotiator: Negotiator? = null): Deferred<OdrlPolicy> {
        val consumerPid = consumerPidMutex.withLock {
            val consumerPid = newProcessId { pid ->
                !(consumerReservedIds.contains(pid) || consumerNegotiations.containsKey(pid))
            }
            consumerReservedIds.add(consumerPid)
            consumerPid
        }

        //       automatic jsonld serialization.
        //       Maybe we can get away with setting the base to the ODRL namespace.
        val odrlRequest = jacksonObjectMapper().convertValue<JsonNode>(request)
        val contractRequestMessage = ContractRequestMessage(
            providerPid = null,
            consumerPid = consumerPid,
            callbackUrl.toString(),
            odrlRequest
        )

        val requestUrl = URLBuilder(provider).apply {
            path("request")
        }.build()

        val resp = httpClient.post(requestUrl) {
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
                consumerPidMutex.withLock { consumerReservedIds.remove(consumerPid) }
                return CompletableDeferred<OdrlPolicy>().apply {
                    cancel("The provider terminated the negotiation")
                }
            }
            else -> {
                consumerPidMutex.withLock { consumerReservedIds.remove(consumerPid) }
                return CompletableDeferred<OdrlPolicy>().apply {
                    cancel("The provider returned an invalid response")
                }
            }
        }

        // Create the final negotiation object now that we have all the information required.
        // Note that we cannot bind the peer identity yet because we don't have a request with an Authorization header
        // at this point.
        val negotiation = Negotiation(
            state = NegotiationState.REQUESTED,
            peerIdentity = null,
            consumerPid = consumerPid,
            providerPid = deserializedResponse.providerPid,
            negotiator = negotiator ?: defaultNegotiator,
            httpClient = httpClient,
            ownEndpoint = callbackUrl,
            peerEndpoint = requestUrl,
        )

        consumerPidMutex.withLock {
            consumerReservedIds.remove(consumerPid)
            consumerNegotiations[consumerPid] = negotiation
        }

        return negotiation.completed
    }

    /**
     * Create a new negotiation object from a consumer's request.
     * If the request contains a PID for an existing negotiation, this function is equivalent to [getAsProvider].
     */
    suspend fun createFromRequest(request: ContractRequestMessage, originUrl: Url, peerIdent: String): Negotiation? {
        if (request.providerPid != null) {
            return getAsProvider(request.providerPid, request.consumerPid, peerIdent)
        }

        providerPidMutex.withLock {
            val providerPid = newProcessId { pid -> !providerNegotiation.containsKey(pid) }

            val negotiation = Negotiation(
                state = NegotiationState.REQUESTED,
                peerIdentity = peerIdent,
                providerPid = providerPid,
                consumerPid = request.consumerPid,
                negotiator = defaultNegotiator,
                httpClient = httpClient,
                ownEndpoint = originUrl,
                peerEndpoint = Url(request.callbackAddress)
            )
            providerNegotiation[providerPid] = negotiation
            return negotiation
        }
    }

    suspend fun createFromOffer(request: ContractOfferMessage, originUrl: Url, peerIdent: String): Negotiation? {
        if (request.consumerPid != null) {
            return getAsConsumer(request.consumerPid, request.providerPid, peerIdent)
        }

        consumerPidMutex.withLock {
            val consumerPid = newProcessId { pid ->
                !(consumerNegotiations.containsKey(pid) || consumerReservedIds.contains(pid))
            }

            val negotiation = Negotiation(
                state = NegotiationState.OFFERED,
                peerIdentity = peerIdent,
                providerPid = request.providerPid,
                consumerPid = consumerPid,
                negotiator = defaultNegotiator,
                httpClient = httpClient,
                ownEndpoint = originUrl,
                peerEndpoint = Url(request.callbackAddress)
            )

            consumerNegotiations[consumerPid] = negotiation
            return negotiation
        }
    }

    fun getAsProvider(providerPid: String, consumerPid: String?, peerIdent: String): Negotiation? {
        val negotiation = providerNegotiation[providerPid] ?: return null

        if (consumerPid != null && negotiation.consumerPid != consumerPid) {
            // While this would be more of a bad request scenario, but we just return not found in order to prevent
            // even the remote possibility of probing for negotiations
            return null
        }

        if (negotiation.peerIdentity != null && negotiation.peerIdentity != peerIdent) {
            return null
        }

        return negotiation
    }

    fun getAsConsumer(consumerPid: String, providerPid: String?, peerIdent: String): Negotiation? {
        val negotiation = consumerNegotiations[consumerPid] ?: return null

        if (providerPid != null && negotiation.providerPid != providerPid) {
            return null
        }

        if (negotiation.peerIdentity != null && negotiation.peerIdentity != peerIdent) {
            return null
        }

        return negotiation
    }

    private fun newProcessId(accept: (String) -> Boolean): String {
        return sequence<UUID> { UUID.randomUUID() }
            .map { uuid -> "urn:uuid:$uuid" }
            .first(accept)
    }
}