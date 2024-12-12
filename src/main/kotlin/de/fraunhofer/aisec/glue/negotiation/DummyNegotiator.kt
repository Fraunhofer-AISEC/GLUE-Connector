package de.fraunhofer.aisec.glue.negotiation

import de.fraunhofer.iosb.odrl.OdrlPolicy
import org.slf4j.LoggerFactory

class DummyNegotiator(disableWarning: Boolean = false) : Negotiator {
    init {
        if (!disableWarning) {
            LOG.warn("Using placeholder dummy negotiator, which should be replaced ASAP")
        }
    }

    override fun evaluateOffer(contractOffer: OdrlPolicy): Pair<Boolean, OdrlPolicy?> {
        return Pair(true, null)
    }

    override fun evaluateRequest(contractRequest: OdrlPolicy): Pair<Boolean, OdrlPolicy?> {
        return Pair(true, null)
    }

    override fun verifyAgreement(
        policyOffer: OdrlPolicy,
        receivedAgreement: OdrlPolicy,
    ): Boolean {
        return true
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(DummyNegotiator::class.java)
    }
}
