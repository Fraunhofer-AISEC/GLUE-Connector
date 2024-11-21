package de.fraunhofer.iosb.odrl.evaluator

import de.fraunhofer.iosb.odrl.OdrlConstraint
import de.fraunhofer.iosb.odrl.OdrlException
import de.fraunhofer.iosb.odrl.OdrlLogicalConstraint
import java.util.function.Predicate

class OdrlConstraintEvaluator(
    private val operatorEvaluator: OdrlOperatorEvaluator = OdrlOperatorEvaluator(),
) {
    fun isSatisfied(
        ctx: Context,
        logicalConstraint: OdrlLogicalConstraint
    ): Result {
        return if (logicalConstraint.or.isNotEmpty())
            isConstraintListSatisfied(ctx, logicalConstraint.or) { results -> results.any { it.success } }
        else if (logicalConstraint.xone.isNotEmpty())
            isConstraintListSatisfied(ctx, logicalConstraint.xone) { results -> results.count { it.success } == 1 }
        else if (logicalConstraint.and.isNotEmpty())
            isConstraintListSatisfied(ctx, logicalConstraint.and) { results -> results.all { it.success } }
        else if (logicalConstraint.andSequence.isNotEmpty())
            isConstraintListSatisfied(ctx, logicalConstraint.andSequence) { results -> results.all { it.success } }
        else
            throw OdrlException("Invalid $logicalConstraint: Expected property 'or', 'xone', 'and', or 'andSequence'")
    }

    private fun isConstraintListSatisfied(
        ctx: Context,
        constraintList: List<OdrlConstraint>,
        predicate: Predicate<List<Result>>
    ): Result {
        val results = constraintList.map { isSatisfied(ctx, it) }
        if (predicate.test(results))
            return Result(true)
        return Result(false, "Not all constraints satisfied. Individual results: $results")
    }

    fun isSatisfied(
        ctx: Context,
        constraint: OdrlConstraint
    ): Result {
        return if (constraint.rightOperand.isNotEmpty())
            operatorEvaluator.evaluateWithRightOperand(ctx, constraint.operator, constraint.leftOperand, constraint.rightOperand)
        else if (constraint.rightOperandReference.isNotEmpty())
            operatorEvaluator.evaluateWithRightOperandReference(ctx, constraint.operator, constraint.leftOperand, constraint.rightOperandReference)
        else
            throw OdrlException("Invalid $constraint: Expected property 'rightOperand' or 'rightOperandReference'")
    }
}