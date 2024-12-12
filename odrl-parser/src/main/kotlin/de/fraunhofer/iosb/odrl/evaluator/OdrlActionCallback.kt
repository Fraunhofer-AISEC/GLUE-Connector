package de.fraunhofer.iosb.odrl.evaluator

import de.fraunhofer.iosb.odrl.OdrlAction

fun interface OdrlActionCallback {
    suspend fun exerciseAction(action: OdrlAction): Result
}