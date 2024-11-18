package fhg.glue.odrl.de.fraunhofer.iosb.odrl.evaluator

import fhg.glue.odrl.de.fraunhofer.iosb.odrl.OdrlAction

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