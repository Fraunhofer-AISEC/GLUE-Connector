package de.fraunhofer.aisec.glue.negotiation.edc

import de.fraunhofer.aisec.glue.models.edc.TerminateNegotiationRequest
import de.fraunhofer.aisec.glue.negotiation.Negotiation
import de.fraunhofer.aisec.glue.negotiation.NegotiationException
import de.fraunhofer.iosb.odrl.OdrlAgreement
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.CompletableDeferred
import java.net.URI
import de.fraunhofer.aisec.glue.models.edc.ContractNegotiation as EdcContractNegotiation
import de.fraunhofer.aisec.glue.models.edc.ContractNegotiation.NegotiationState as EdcNegotiationState

class EdcNegotiation(
    val completed: CompletableDeferred<OdrlAgreement>,
    val httpClient: HttpClient,
    val edcId: String,
    val peerId: String,
    val edcUrl: String,
    var edcState: EdcNegotiationState = EdcNegotiationState.INITIAL,
) : Negotiation {

    // We don't support provider negotiation with the EDC
    override val role: Negotiation.Role
        get() = Negotiation.Role.CONSUMER

    override val id: String
        get() = edcId

    override val peerIdentity: String
        get() = peerId

    override val state: String
        get() = edcState.toString()

    override suspend fun cancel(cause: String?) {
        httpClient.post("$edcUrl/management/v3/contractnegotiations/$edcId/terminate") {
            contentType(ContentType.Application.Json)
            setBody(
                TerminateNegotiationRequest(
                    uid = URI(edcId),
                    reason = cause ?: "Negotiation was terminated by user",
                ),
            )
        }
    }

    fun update(edcObject: EdcContractNegotiation) {
        if (edcObject.state == edcState) {
            // No change, nothing to do
            return
        }

        LOG.atDebug {
            message = "State change in EDC negotiation"
            payload = mapOf("id" to edcId, "oldState" to edcState, "newState" to edcObject.state)
        }

        edcState = edcObject.state
        when (edcObject.state) {
            EdcNegotiationState.FINALIZED -> {
                completed.complete(TODO("Fetch agreement"))
            }
            EdcNegotiationState.TERMINATED -> {
                completed.completeExceptionally(
                    NegotiationException("EDC negotiation terminated: ${edcObject.errorDetail ?: "reason unknown"}"),
                )
            }
            else -> {}
        }
    }

    companion object {
        private val LOG = KotlinLogging.logger {}
    }
}
