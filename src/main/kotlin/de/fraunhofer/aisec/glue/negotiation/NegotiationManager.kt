package de.fraunhofer.aisec.glue.negotiation

import de.fraunhofer.iosb.odrl.OdrlPolicy

abstract class NegotiationManager {
    /**
     * Return all currently ongoing negotiations.
     */
    abstract suspend fun getNegotiations(): Collection<Negotiation>

    /**
     * Get the negotiation with the provided ID.
     */
    abstract suspend fun getNegotiation(id: String): Negotiation?

    /**
     * Start a new negotiation with a peer connector and an arbitrary ODRL policy.
     * @param peer The URL or an internal ID for the peer to negotiate with. The format for internal IDs is
     * implementation defined.
     * @param offer The contract to propose to the peer. If this contract is not an ODRL offer, it is converted to one
     * using the current connector's identity.
     * @return The newly created negotiation object.
     */
    abstract suspend fun startNegotiation(
        peer: String,
        offer: OdrlPolicy,
    ): Negotiation
}
