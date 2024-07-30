package de.fraunhofer.aisec.glue.negotiation

import fhg.glue.odrl.OdrlPolicy
import org.slf4j.LoggerFactory

class DummyNegotiator : Negotiator {
    init {
        LOG.warn("Using placeholder dummy negotiator, which should be replaced ASAP")
    }

    override fun evaluateOffer(contractOffer: OdrlPolicy): Pair<Boolean, OdrlPolicy?> {
        return Pair(true, null)
    }

    override fun evaluateRequest(contractRequest: OdrlPolicy): Pair<Boolean, OdrlPolicy?> {
        return Pair(true, null)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(DummyNegotiator::class.java)
    }
}