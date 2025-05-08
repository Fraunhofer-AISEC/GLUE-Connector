package de.fraunhofer.aisec.glue.negotiation

import de.fraunhofer.aisec.glue.models.negotiation.ContractNegotiation
import de.fraunhofer.aisec.glue.models.negotiation.NegotiationState

interface Negotiation {
    val state: NegotiationState
    val role: Role
    val ownPid: String
    val peerIdentity: String?

    /**
     * Get the current state of the negotiation encoded as a contract negotiation state message
     */
    suspend fun getStateMessage(): ContractNegotiation

    /**
     * Cancel this negotiation
     */
    suspend fun cancel(cause: String?)

    enum class Role {
        PROVIDER,
        CONSUMER,
    }
}
