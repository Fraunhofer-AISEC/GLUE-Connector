package fhg.glue.odrl.de.fraunhofer.iosb.odrl.evaluator

import fhg.glue.odrl.de.fraunhofer.iosb.odrl.OdrlAction

fun interface OdrlActionCallback {
    suspend fun exerciseAction(action: OdrlAction): Result
}