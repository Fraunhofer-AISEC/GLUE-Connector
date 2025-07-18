package de.fraunhofer.aisec.glue.negotiation

import de.fraunhofer.iosb.odrl.OdrlAgreement
import de.fraunhofer.iosb.odrl.OdrlPolicy
import kotlinx.coroutines.Deferred

abstract class NegotiationManager {
    /**
     * Return all currently ongoing negotiations.
     */
    abstract suspend fun getNegotiations(): Collection<Negotiation>

    /**
     * Start a new negotiation with a peer connector and an arbitrary ODRL policy.
     * @param peer The URL or an internal ID for the peer to negotiate with. The format for internal IDs is
     * implementation defined.
     * @param offer The contract to propose to the peer. If this contract is not an ODRL offer, it is converted to one
     * using the current connector's identity.
     * @return A future that returns the negotiated contract. It is cancelled if the negotiation fails.
     */
    abstract suspend fun startNegotiation(
        peer: String,
        offer: OdrlPolicy,
    ): Deferred<OdrlAgreement>
}
