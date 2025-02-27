package de.fraunhofer.iosb.odrl.evaluator

import de.fraunhofer.iosb.odrl.OdrlAction
import de.fraunhofer.iosb.odrl.OdrlConstraint
import de.fraunhofer.iosb.odrl.OdrlException
import de.fraunhofer.iosb.odrl.OdrlLogicalConstraint

class OdrlActionEvaluator(
    private val constraintEvaluator: OdrlConstraintEvaluator = OdrlConstraintEvaluator(),
) {
    fun matches(
        ctx: Context,
        action: OdrlAction,
    ): Result {
        if (ctx.params["actionUid"] != action.value.uid)
            return Result(false, "Context uid ${ctx.params["actionUid"]} does not match action uid ${action.value.uid}")

        // Refinements have to be satisfied
        for (r in action.refinement) {
            val result =
                when (r) {
                    is OdrlConstraint -> constraintEvaluator.isSatisfied(ctx, r)
                    is OdrlLogicalConstraint -> constraintEvaluator.isSatisfied(ctx, r)
                    else -> throw OdrlException("Invalid $r: Expected type 'OdrlConstraint' or 'OdrlLogicalConstraint'")
                }
            if (!result.success)
                return Result(result, "$r is not satisfied")
        }

        return Result(true)
    }

    suspend fun exercise(
        ctx: Context,
        action: OdrlAction,
        actionCallback: OdrlActionCallback? = null
    ): Result {

        if (actionCallback == null)
            return Result(false, "Cannot exercise $action because no action callback was provided")
        return actionCallback.exerciseAction(action)
    }
}
