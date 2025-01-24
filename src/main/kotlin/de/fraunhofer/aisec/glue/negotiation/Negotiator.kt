package de.fraunhofer.aisec.glue.negotiation

import de.fraunhofer.iosb.odrl.OdrlAgreement
import de.fraunhofer.iosb.odrl.OdrlOffer

interface Negotiator {
    /**
     * Evaluate a contract offer sent by a resource provider.
     * If the offer is acceptable, return true.
     * Otherwise, return false and optionally a new contract request to send to the provider instead.
     *
     * More formally, returning true will move the negotiation into the `accepted` state.
     * Returning false and a new contract request message will move the negotiation into the `requested` stage.
     * Returning false without a new contract request will terminate the negotiation.
     */
    fun evaluateOffer(contractOffer: OdrlOffer): Pair<Boolean, OdrlOffer?>

    /**
     * Evaluate a given contract request sent by a resource consumer.
     * If the terms in the request are agreeable as-is, return true.
     * If not, return false and optionally a new contract offer to be sent to the consumer.
     *
     * Returning true from this function will move the negotiation into the `agreed` state directly.
     * Returning false with a new contract offer will transition the negotiation into the `offered` state.
     * Otherwise, the negotiation is terminated.
     */
    fun evaluateRequest(contractRequest: OdrlOffer): Pair<Boolean, OdrlOffer?> // TODO: Replace Policy with types for request and offer

    /**
     * Determine if the received agreement is still semantically the same as the policy offer.
     */
    fun verifyAgreement(
        policyOffer: OdrlOffer,
        receivedAgreement: OdrlAgreement,
    ): Boolean
}
