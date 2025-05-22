package de.fraunhofer.aisec.glue.negotiation.edc

import com.fasterxml.jackson.databind.node.ObjectNode
import de.fraunhofer.aisec.glue.models.edc.ContractNegotiation
import de.fraunhofer.aisec.glue.models.edc.ContractRequest
import de.fraunhofer.aisec.glue.models.edc.ContractRequestResponse
import de.fraunhofer.aisec.glue.models.edc.QuerySpec
import de.fraunhofer.aisec.glue.negotiation.NegotiationManager
import de.fraunhofer.aisec.glue.negotiation.toOffer
import de.fraunhofer.iosb.odrl.OdrlAgreement
import de.fraunhofer.iosb.odrl.OdrlParty
import de.fraunhofer.iosb.odrl.OdrlPolicy
import de.fraunhofer.iosb.odrl.parser.OdrlParser
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

class EdcNegotiationManager(
    private val httpClient: HttpClient,
    private val odrlParser: OdrlParser,
    private val baseUrl: String = "http://localhost:19193",
) : NegotiationManager() {
    private val negotiations = ConcurrentHashMap<URI, EdcNegotiation>()

    // Some kind of cron-like scheduler would be nice
    private val pollStateJob: Job =
        CoroutineScope(Dispatchers.Default).launch {
            while (true) {
                try {
                    syncWithEdc()
                } catch (e: Exception) {
                    LOG.atWarn {
                        message = "Exception during scheduled edc update task"
                        cause = e
                    }
                }
                delay(30.seconds)
            }
        }

    private suspend fun syncWithEdc() {
        val edcQueryUrl = "$baseUrl/management/v3/contractnegotiations/request"
        LOG.atDebug {
            message = "Running scheduled EDC negotiation state update"
            payload = mapOf("requestUrl" to edcQueryUrl)
        }

        // While there is filtering in the EDC, it does not seem possible to annotate negotiation objects with some
        // marker that allows us to only fetch our negotiations.
        // Instead, we just get all and filter on our end (This query is implemented as a post request).
        val edcQueryResp =
            httpClient.post(edcQueryUrl) {
                contentType(ContentType.Application.Json)
                setBody(QuerySpec(filterExpression = listOf()))
            }

        if (!edcQueryResp.status.isSuccess()) {
            val body = edcQueryResp.bodyAsText()
            LOG.atWarn {
                message = "Received error response from scheduled EDC negotiation state query"
                payload = mapOf("requestUrl" to edcQueryUrl, "status" to edcQueryResp.status, "body" to body)
            }
            return
        }

        //  needed.
        val negotiationStates = edcQueryResp.body<List<ContractNegotiation>>()
        LOG.atDebug {
            message = "EDC state update"
            payload = mapOf("jsonState" to negotiationStates)
        }

        for (state in negotiationStates) {
            val negotiation = negotiations[state.uid] ?: continue
            negotiation.update(state)
        }
    }

    override suspend fun getNegotiations(): Collection<EdcNegotiation> = negotiations.values

    private suspend fun makeHttpException(
        resp: HttpResponse,
        body: String? = null,
    ): ResponseException {
        assert(!resp.status.isSuccess())
        val ctor =
            when (resp.status.value) {
                in 300..399 -> ::RedirectResponseException
                in 400..499 -> ::ClientRequestException
                in 500..599 -> ::ServerResponseException
                else -> ::ResponseException
            }
        return ctor(resp, body ?: resp.bodyAsText())
    }

    override suspend fun startNegotiation(
        peer: String,
        offer: OdrlPolicy,
    ): Deferred<OdrlAgreement> {
        val compactedSerializedOffer =
            odrlParser.mapper.writeValueAsString(
                offer.toOffer(odrlParser, OdrlParty("todo://add-connector-id-from-config-here")),
            )
        val compactedOffer = odrlParser.mapper.readTree(compactedSerializedOffer) as ObjectNode
        println("Before pushdown: $compactedSerializedOffer")
        EdcQuirks.pushDownAction(compactedOffer)
        EdcQuirks.unwrapRdfValue(compactedOffer)
        println("After pushdown: ${odrlParser.mapper.writeValueAsString(compactedOffer)}")

        val contractRequest =
            ContractRequest(
                peer,
                "dataspace-protocol-http",
                compactedOffer,
            )
        val createResp =
            httpClient.post("$baseUrl/management/v3/contractnegotiations") {
                contentType(ContentType.Application.Json)
                setBody(contractRequest)
            }
        if (!createResp.status.isSuccess()) {
            return CompletableDeferred<OdrlAgreement>().apply {
                completeExceptionally(makeHttpException(createResp))
            }
        }

        val contractResponse = createResp.body<ContractRequestResponse>()
        val contractFuture = CompletableDeferred<OdrlAgreement>()

        val negotiation =
            EdcNegotiation(
                contractFuture,
                httpClient,
                edcId = contractResponse.uid.toString(),
                peerId = peer,
                edcUrl = baseUrl,
            )
        negotiations[contractResponse.uid] = negotiation

        return contractFuture
    }

    companion object {
        private val LOG = KotlinLogging.logger {}
    }
}
