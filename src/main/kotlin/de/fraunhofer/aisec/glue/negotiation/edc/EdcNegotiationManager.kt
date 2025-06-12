package de.fraunhofer.aisec.glue.negotiation.edc

import com.fasterxml.jackson.databind.node.ObjectNode
import de.fraunhofer.aisec.glue.models.edc.ContractNegotiation
import de.fraunhofer.aisec.glue.models.edc.ContractRequest
import de.fraunhofer.aisec.glue.models.edc.ContractRequestResponse
import de.fraunhofer.aisec.glue.models.edc.QuerySpec
import de.fraunhofer.aisec.glue.negotiation.NegotiationException
import de.fraunhofer.aisec.glue.negotiation.NegotiationManager
import de.fraunhofer.iosb.odrl.OdrlAgreement
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
import java.util.*
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
            // TODO: Maybe keep count of failures and error out at some point?
            return
        }

        // TODO: For each state object, decode the result and update local objects, triggering completion events as
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

    private fun convertToOffer(policy: OdrlPolicy): Result<Pair<OdrlPolicy, String?>> {
        // The required format is `[contractId]:[target]:[uuid]` with all three parts being base64 encoded.
        // If this format is already there, we do nothing and hope for the best.
        // Note that we do not check if the individual components are valid.
        val segments = policy.uid.toString().split(':')
        if (segments.size == 3 && segments.all { runCatching { Base64.getDecoder().decode(it) }.isSuccess }) {
            LOG.atTrace {
                message = "Contract ID is already in the expected format"
                payload = mapOf("contractId" to policy.uid)
            }
            return Result.success(odrlParser.convertToOdrlOffer(policy) to null)
        }

        val contractId = policy.uid.toString()
        val uuid = UUID.randomUUID().toString()

        val targets = policy.permission.mapNotNull { p -> p.target?.uid }.distinct()
        val target = when (targets.size) {
            0 -> {
                LOG.atDebug {
                    message = "Cannot convert policy to valid EDC offer as it contains no targets"
                    payload = mapOf("contractId" to policy.uid)
                }
                return Result.failure(NegotiationException("Policy contains no targets"))
            }
            1 -> targets[0].toString()
            else -> {
                LOG.atDebug {
                    message = "Cannot convert policy to valid EDC offer as its target is ambiguous"
                    payload = mapOf("contractId" to policy.uid, "candidates" to targets.map(URI::toString))
                }
                return Result.failure(NegotiationException("Policy target is ambiguous, specify the ID in EDC format"))
            }
        }

        val newId =
            arrayOf(contractId, target, uuid).joinToString(":") { Base64.getEncoder().encodeToString(it.toByteArray()) }
        return Result.success(odrlParser.convertToOdrlOffer(policy) to newId)
    }

    override suspend fun startNegotiation(
        peer: String,
        offer: OdrlPolicy,
    ): Deferred<OdrlAgreement> {
        val (offer, newId) = convertToOffer(offer/*, OdrlParty("todo://add-connector-od-from-config-here")*/).getOrElse {
            return CompletableDeferred<OdrlAgreement>().apply {completeExceptionally(it)}
        }

        val compactedSerializedOffer = odrlParser.mapper.writeValueAsString(offer)

        // The EDC requires a specific format for the contract request ID to correlate it with the asset and contract
        // definitions it stores.
        // We construct this identifier out of the present contract id and try to extract the target from the given
        // policy's permissions.
        // If the contract id is already in the expected format, we just pass it as-is.

        val compactedOffer = odrlParser.mapper.readTree(compactedSerializedOffer) as ObjectNode
        // Some hacks to convert the compact serialization to the format required by the edc
        EdcQuirks.pushDownAction(compactedOffer)
        EdcQuirks.unwrapRdfValue(compactedOffer)
        EdcQuirks.rewriteTypeFields(compactedOffer)
        EdcQuirks.stringifyGlueKeys(odrlParser.mapper, compactedOffer)
        // This one is even worse, but the URI type in the model does not allow the value required by the edc
        if (newId != null) {
            compactedOffer.put("uid", newId)
        }

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

        // TODO: Other setup stuff?
        val negotiation =
            EdcNegotiation(
                contractFuture,
                httpClient,
                edcId = contractResponse.uid.toString(),
                peerId = peer,
                edcUrl = baseUrl,
            )
        // TODO: Atomically check if the uid is reused
        negotiations[contractResponse.uid] = negotiation

        return contractFuture
    }

    companion object {
        private val LOG = KotlinLogging.logger {}
    }
}
