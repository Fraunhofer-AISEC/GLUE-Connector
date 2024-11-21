package de.fraunhofer.iosb.odrl.evaluator

import de.fraunhofer.iosb.odrl.OdrlAction

fun interface OdrlInfringementCallback {
    suspend fun isActionInfringed(action: OdrlAction): Boolean
}