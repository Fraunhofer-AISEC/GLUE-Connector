package fhg.glue.odrl.de.fraunhofer.iosb.odrl.evaluator

import fhg.glue.odrl.de.fraunhofer.iosb.odrl.OdrlConstraint
import fhg.glue.odrl.de.fraunhofer.iosb.odrl.OdrlDuty
import fhg.glue.odrl.de.fraunhofer.iosb.odrl.OdrlException
import fhg.glue.odrl.de.fraunhofer.iosb.odrl.OdrlLogicalConstraint

class OdrlDutyEvaluator(
    private val actionEvaluator: OdrlActionEvaluator = OdrlActionEvaluator(),
    private val assetEvaluator: OdrlAssetEvaluator = OdrlAssetEvaluator(),
    private val constraintEvaluator: OdrlConstraintEvaluator = OdrlConstraintEvaluator(),
) {
    fun isFulfilled(
        ctx: Context,
        duty: OdrlDuty,
        actionCallback: OdrlActionCallback? = null
    ): Result {
        var result: Result

        // If target does not match, mark duty as fulfilled
        if (duty.target != null) {
            if (!assetEvaluator.matches(ctx, duty.target).success)
                return Result(true)
        }

        // If not all constraints are satisfied, mark duty as fulfilled
        for (c in duty.constraint) {
            result =
                when (c) {
                    is OdrlConstraint -> constraintEvaluator.isSatisfied(ctx, c)
                    is OdrlLogicalConstraint -> constraintEvaluator.isSatisfied(ctx, c)
                    else -> throw OdrlException("Invalid $c: Expected type 'OdrlConstraint' or 'OdrlLogicalConstraint'")
                }
            if (!result.success)
                return Result(true);
        }

        // Action must be exercised
        result = actionEvaluator.exercise(ctx, duty.action, actionCallback)
        if (!result.success) {
            result = Result(result, "${duty.action} was not exercised successfully")

            // If action has not been successfully exercised, consequences have to be fulfilled
            val results = duty.consequence.map { isFulfilled(ctx, it, actionCallback) }
            if (results.any { !it.success })
                return Result(result, "Not all consequences fulfilled. Individual results: $results")
            return result;
        }

        return Result(true)
    }
}