package de.fraunhofer.aisec.glue.negotiation

import de.fraunhofer.iosb.odrl.OdrlAgreement

interface Negotiation {
    val role: Role
    val id: String
    val peerIdentity: String?
    val state: String

    /**
     * Cancel this negotiation
     */
    suspend fun cancel(cause: String?)

    suspend fun awaitCompletion(): OdrlAgreement

    enum class Role {
        PROVIDER,
        CONSUMER,
    }
}
