package de.fraunhofer.iosb.odrl.evaluator

import de.fraunhofer.iosb.odrl.*

class OdrlOperatorEvaluator(
    private val leftOperandEvaluator: OdrlLeftOperandEvaluator = OdrlLeftOperandEvaluator(),
) {
    fun evaluateWithRightOperandReference(
        ctx: Context,
        operator: OdrlOperator,
        leftOperand: OdrlLeftOperand,
        rightOperandReferenceList: List<XmlAnyUri>
    ): Result {
        throw OdrlException("Evaluation of right operand references is not supported")
    }

    fun evaluateWithRightOperand(
        ctx: Context,
        operator: OdrlOperator,
        leftOperand: OdrlLeftOperand,
        rightOperandList: List<OdrlRightOperand>
    ): Result {
        if (rightOperandList.size == 1)
            return evaluate(ctx, operator, leftOperand, rightOperandList[0])

        val leftOperandValue = leftOperandEvaluator.evaluate(ctx, leftOperand)
        when (operator.uid.toString()) {
            OdrlIdentifiers.EQ -> {
                if (leftOperandValue !is Collection<*>)
                    throw OdrlException("Cannot evaluate $operator on left operand $leftOperandValue and right operand list $rightOperandList")
                return Result(leftOperandValue == rightOperandList, "$leftOperand != $rightOperandList")
            }
            OdrlIdentifiers.NEQ -> {
                if (leftOperandValue !is Collection<*>)
                    throw OdrlException("Cannot evaluate $operator on left operand $leftOperandValue and right operand list $rightOperandList")
                return Result(leftOperandValue != rightOperandList, "$leftOperand == $rightOperandList")
            }
            OdrlIdentifiers.ISPARTOF ->
                return Result(rightOperandList.contains(leftOperandValue), "$leftOperand is not a part of $rightOperandList")
            OdrlIdentifiers.GT, OdrlIdentifiers.GTEQ, OdrlIdentifiers.LT, OdrlIdentifiers.LTEQ ->
                throw OdrlException("Cannot evaluate $operator on left operand $leftOperandValue and right operand list $rightOperandList")
            OdrlIdentifiers.ISA, OdrlIdentifiers.ISALLOF, OdrlIdentifiers.ISANYOF, OdrlIdentifiers.ISNONEOF ->
                throw OdrlException("$operator is not supported")
        }
        throw OdrlException("Invalid $operator")
    }

    private fun evaluate(
        ctx: Context,
        operator: OdrlOperator,
        leftOperand: OdrlLeftOperand,
        rightOperand: OdrlRightOperand
    ): Result {
        val leftOperandValue = leftOperandEvaluator.evaluate(ctx, leftOperand)
        when (operator.uid.toString()) {
            OdrlIdentifiers.EQ -> return Result(leftOperandValue == rightOperand, "$leftOperand != $rightOperand")
            OdrlIdentifiers.NEQ -> return Result(leftOperandValue != rightOperand, "$leftOperand == $rightOperand")
            OdrlIdentifiers.GT -> return Result(leftOperandValue > rightOperand, "$leftOperand <= $rightOperand")
            OdrlIdentifiers.GTEQ -> return Result(leftOperandValue >= rightOperand, "$leftOperand < $rightOperand")
            OdrlIdentifiers.LT -> return Result(leftOperandValue < rightOperand, "$leftOperand >= $rightOperand")
            OdrlIdentifiers.LTEQ -> return Result(leftOperandValue <= rightOperand, "$leftOperand > $rightOperand")
            OdrlIdentifiers.HASPART -> {
                if (leftOperandValue !is Collection<*>)
                    throw OdrlException("Cannot evaluate $operator on left operand $leftOperandValue")
                return Result(leftOperandValue.contains(rightOperand), "$leftOperand has no part of $rightOperand")
            }
            OdrlIdentifiers.ISA, OdrlIdentifiers.ISALLOF, OdrlIdentifiers.ISANYOF, OdrlIdentifiers.ISNONEOF ->
                throw OdrlException("$operator is not supported")
        }
        throw OdrlException("Invalid $operator")
    }
}