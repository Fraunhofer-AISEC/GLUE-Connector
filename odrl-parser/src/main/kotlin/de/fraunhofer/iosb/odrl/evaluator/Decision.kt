package de.fraunhofer.iosb.odrl.evaluator

import de.fraunhofer.iosb.odrl.OdrlAction

data class Decision(
    val evaluationResult: Result,
    val infringementViolations: List<OdrlAction>,
) {
    override fun toString(): String {
        var string = if (evaluationResult.success) "ALLOW" else "DENY"
        if (infringementViolations.isNotEmpty())
            string += " with violations $infringementViolations"
        return string
    }
}