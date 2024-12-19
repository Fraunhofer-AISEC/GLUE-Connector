package de.fraunhofer.aisec.glue.negotiation

import de.fraunhofer.iosb.odrl.OdrlAgreement
import de.fraunhofer.iosb.odrl.OdrlOffer
import org.slf4j.LoggerFactory

class DummyNegotiator(disableWarning: Boolean = false) : Negotiator {
    init {
        if (!disableWarning) {
            LOG.warn("Using placeholder dummy negotiator, which should be replaced ASAP")
        }
    }

    override fun evaluateOffer(contractOffer: OdrlOffer): Pair<Boolean, OdrlOffer?> {
        return Pair(true, null)
    }

    override fun evaluateRequest(contractRequest: OdrlOffer): Pair<Boolean, OdrlOffer?> {
        return Pair(true, null)
    }

    override fun verifyAgreement(
        policyOffer: OdrlOffer,
        receivedAgreement: OdrlAgreement,
    ): Boolean {
        return true
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(DummyNegotiator::class.java)
    }
}
