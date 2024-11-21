package fhg.glue.odrl.de.fraunhofer.iosb.odrl.evaluator

import fhg.glue.odrl.de.fraunhofer.iosb.odrl.OdrlAction

fun interface OdrlInfringementCallback {
    suspend fun isActionInfringed(action: OdrlAction): Boolean
}