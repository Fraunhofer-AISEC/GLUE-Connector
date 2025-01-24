package de.fraunhofer.aisec.glue.negotiation

import de.fraunhofer.aisec.glue.models.negotiation.*

interface Negotiation {
    val role: Role
    val peerIdentity: String?

    /**
     * Cancel this negotiation
     */
    suspend fun cancel(cause: String?)

    suspend fun processRequest(
        req: ContractRequestMessage,
        initial: Boolean = false,
        responseCallback: suspend (Boolean) -> Unit,
    )

    /**
     * Process a contract offer sent from the provider.
     * This operation is only allowed if we are a consumer and the negotiation is either in its initial or the requested
     * state, which will be reported back by the argument to [responseCallback].
     */
    suspend fun processOffer(
        req: ContractOfferMessage,
        initial: Boolean = false,
        responseCallback: suspend (Boolean) -> Unit,
    )

    suspend fun processAccept(responseCallback: suspend (Boolean) -> Unit): Unit

    suspend fun processVerify(responseCallback: suspend (Boolean) -> Unit): Unit

    suspend fun processFinalize(responseCallback: suspend (Boolean) -> Unit)

    fun getStateMessageUnlocked(): ContractNegotiation

    suspend fun getStateMessage(): ContractNegotiation

    suspend fun processAgreement(
        req: ContractAgreementMessage,
        responseCallback: suspend (Boolean) -> Unit,
    )

    suspend fun processTermination(
        req: ContractNegotiationTerminationMessage,
        responseCallback: suspend () -> Unit,
    )

    enum class Role {
        PROVIDER,
        CONSUMER,
    }
}
