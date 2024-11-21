package fhg.glue.odrl.de.fraunhofer.iosb.odrl.evaluator

import fhg.glue.odrl.de.fraunhofer.iosb.odrl.OdrlConstraint
import fhg.glue.odrl.de.fraunhofer.iosb.odrl.OdrlException
import fhg.glue.odrl.de.fraunhofer.iosb.odrl.OdrlLogicalConstraint
import fhg.glue.odrl.de.fraunhofer.iosb.odrl.OdrlProhibition

class OdrlProhibitionEvaluator(
    private val actionEvaluator: OdrlActionEvaluator = OdrlActionEvaluator(),
    private val assetEvaluator: OdrlAssetEvaluator = OdrlAssetEvaluator(),
    private val constraintEvaluator: OdrlConstraintEvaluator = OdrlConstraintEvaluator(),
    private val dutyEvaluator: OdrlDutyEvaluator = OdrlDutyEvaluator(),
) {
    fun isDisallowed(
        ctx: Context,
        prohibition: OdrlProhibition
    ): Result {
        // Action has to match
        var result = actionEvaluator.matches(ctx, prohibition.action)
        if (!result.success)
            return Result(result, "${prohibition.action} does not match")

        // Target has to match, if existing
        if (prohibition.target != null) {
            result = assetEvaluator.matches(ctx, prohibition.target)
            if (!result.success)
                return Result(result, "${prohibition.target} does not match")
        } else {
            throw OdrlException("Invalid $prohibition: Expected property 'target'")
        }

        // Constraints have to be satisfied
        for (c in prohibition.constraint) {
            result =
                when (c) {
                    is OdrlConstraint -> constraintEvaluator.isSatisfied(ctx, c)
                    is OdrlLogicalConstraint -> constraintEvaluator.isSatisfied(ctx, c)
                    else -> throw OdrlException("Invalid $c: Expected type 'OdrlConstraint' or 'OdrlLogicalConstraint'")
                }
            if (!result.success)
                return Result(result, "$c is not satisfied")
        }

        return Result(true) // This prohibition disallows context |ctx|
    }

    suspend fun isNotInfringed(
        ctx: Context,
        prohibition: OdrlProhibition,
        actionCallback: OdrlActionCallback? = null,
        infringementCallback: OdrlInfringementCallback? = null
    ): Result {
        // Remedies have to be fulfilled if action is infringed
        if (infringementCallback == null || infringementCallback.isActionInfringed(prohibition.action)) {
            for (r in prohibition.remedy) {
                val result = dutyEvaluator.isFulfilled(ctx, r, actionCallback)
                if (!result.success)
                    return Result(result, "$r is not fulfilled")
            }
        }

        return Result(true) // The action of this prohibition is not infringed or the remedies are fulfilled
    }
}