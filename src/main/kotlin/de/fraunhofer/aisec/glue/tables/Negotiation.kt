package de.fraunhofer.aisec.glue.tables

import de.fraunhofer.aisec.glue.models.negotiation.NegotiationState
import de.fraunhofer.aisec.glue.negotiation.Negotiation.Role
import de.fraunhofer.iosb.odrl.OdrlPolicy
import org.ktorm.entity.Entity
import java.util.UUID
import kotlin.time.Instant

interface Negotiation : Entity<Negotiation> {
    companion object : Entity.Factory<Negotiation>()

    val id: UUID
    val role: Role
    val peer: String?
    val peerPid: String
    val startTimestamp: Instant
    val state: NegotiationState
    val callbackUrl: String
    val peerUrl: String
    val contract: OdrlPolicy?
    val failureReason: String?
    val lastReceivedMessage: String?
    val nextSentMessage: Int
}
