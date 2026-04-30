package de.fraunhofer.aisec.glue.negotiation.builtin

import de.fraunhofer.aisec.glue.models.negotiation.NegotiationState
import de.fraunhofer.aisec.glue.negotiation.Negotiation
import de.fraunhofer.iosb.odrl.OdrlPolicy
import io.ktor.http.Url
import java.util.UUID
import kotlin.time.Instant

/**
 * Represents data for built-in negotiation operations.
 */
data class BuiltinNegotiationData(
    val id: UUID,
    val role: Negotiation.Role,
    val peer: String?,
    val peerPid: String,
    val startTimestamp: Instant,
    val state: NegotiationState,
    val callbackUrl: Url,
    val peerUrl: Url,
    val contract: OdrlPolicy? = null,
    val failureReason: String? = null,
    val lastReceivedMessage: String? = null,
    val nextSentMessage: Int = 0,
    //  If we want to keep the Negotiator interface, a reference to its implementation would be required here.
) {
    override fun toString(): String {
        val contractString = if (contract != null) "(...)" else "null"
        return "BuiltinNegotiationData(" +
                "id=$id, " +
                "role=$role, " +
                "peer=$peer, " +
                "peerPid=$peerPid, " +
                "startTimestamp=$startTimestamp, " +
                "state=$state, " +
                "callbackUrl=$callbackUrl, " +
                "peerUrl=$peerUrl, " +
                "contract=$contractString, " +
                "failureReason=$failureReason, " +
                "lastReceivedMessage=$lastReceivedMessage, " +
                "nextSentMessage=$nextSentMessage" +
                ")"
    }
}
