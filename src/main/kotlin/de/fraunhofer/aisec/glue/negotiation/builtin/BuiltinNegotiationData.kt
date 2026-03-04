package de.fraunhofer.aisec.glue.negotiation.builtin

import de.fraunhofer.aisec.glue.models.negotiation.NegotiationState
import de.fraunhofer.aisec.glue.negotiation.Negotiation
import de.fraunhofer.iosb.odrl.OdrlPolicy
import io.ktor.http.Url
import kotlin.time.Instant

/**
 * Represents data for built-in negotiation operations.
 */
data class BuiltinNegotiationData(
    val id: String,
    val role: Negotiation.Role,
    val peer: String?,
    val peerPid: String,
    val startTimestamp: Instant,
    val state: NegotiationState,
    val callbackUrl: Url,
    val peerUrl: Url,
    val contract: OdrlPolicy? = null,
    val failureReason: String? = null,
    // TODO: Anything else we want to persist?
    //  If we want to keep the Negotiator interface, a reference to its implementation would be required here.
)
