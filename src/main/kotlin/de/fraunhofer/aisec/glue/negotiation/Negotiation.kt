package de.fraunhofer.aisec.glue.negotiation

interface Negotiation {
    val role: Role
    val id: String
    val peerIdentity: String?
    val state: String

    /**
     * Cancel this negotiation
     */
    suspend fun cancel(cause: String?)

    enum class Role {
        PROVIDER,
        CONSUMER,
    }
}
