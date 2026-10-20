/*
 * Copyright 2024-2025 Fraunhofer AISEC and Fraunhofer IOSB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.fraunhofer.iosb.odrl.evaluator

import de.fraunhofer.iosb.odrl.*

class OdrlOperatorEvaluator(
    private val leftOperandEvaluator: OdrlLeftOperandEvaluator = OdrlLeftOperandEvaluator(),
) {
    fun evaluateWithRightOperandReference(
        ctx: Context,
        operator: OdrlOperator,
        leftOperand: OdrlLeftOperand,
        rightOperandReferenceList: List<XmlAnyUri>,
    ): Result = throw OdrlException("Evaluation of right operand references is not supported")

    fun evaluateWithRightOperand(
        ctx: Context,
        operator: OdrlOperator,
        leftOperand: OdrlLeftOperand,
        rightOperandList: List<OdrlRightOperand>,
    ): Result {
        if (rightOperandList.size == 1) {
            return evaluate(ctx, operator, leftOperand, rightOperandList[0])
        }

        val leftOperandValue = leftOperandEvaluator.evaluate(ctx, leftOperand)
        when (operator.uid.toString()) {
            OdrlIdentifiers.EQ -> {
                if (leftOperandValue !is Collection<*>) {
                    throw OdrlException(
                        "Cannot evaluate $operator on left operand $leftOperandValue and right operand list $rightOperandList",
                    )
                }
                return Result(leftOperandValue == rightOperandList, "$leftOperand != $rightOperandList")
            }

            OdrlIdentifiers.NEQ -> {
                if (leftOperandValue !is Collection<*>) {
                    throw OdrlException(
                        "Cannot evaluate $operator on left operand $leftOperandValue and right operand list $rightOperandList",
                    )
                }
                return Result(leftOperandValue != rightOperandList, "$leftOperand == $rightOperandList")
            }

            OdrlIdentifiers.ISPARTOF -> {
                return Result(rightOperandList.contains(leftOperandValue), "$leftOperand is not a part of $rightOperandList")
            }

            OdrlIdentifiers.GT, OdrlIdentifiers.GTEQ, OdrlIdentifiers.LT, OdrlIdentifiers.LTEQ -> {
                throw OdrlException("Cannot evaluate $operator on left operand $leftOperandValue and right operand list $rightOperandList")
            }

            OdrlIdentifiers.ISA, OdrlIdentifiers.ISALLOF, OdrlIdentifiers.ISANYOF, OdrlIdentifiers.ISNONEOF -> {
                throw OdrlException("$operator is not supported")
            }
        }
        throw OdrlException("Invalid $operator")
    }

    private fun evaluate(
        ctx: Context,
        operator: OdrlOperator,
        leftOperand: OdrlLeftOperand,
        rightOperand: OdrlRightOperand,
    ): Result {
        val leftOperandValue = leftOperandEvaluator.evaluate(ctx, leftOperand)
        when (operator.uid.toString()) {
            OdrlIdentifiers.EQ -> {
                return Result(leftOperandValue == rightOperand, "$leftOperand != $rightOperand")
            }

            OdrlIdentifiers.NEQ -> {
                return Result(leftOperandValue != rightOperand, "$leftOperand == $rightOperand")
            }

            OdrlIdentifiers.GT -> {
                return Result(leftOperandValue > rightOperand, "$leftOperand <= $rightOperand")
            }

            OdrlIdentifiers.GTEQ -> {
                return Result(leftOperandValue >= rightOperand, "$leftOperand < $rightOperand")
            }

            OdrlIdentifiers.LT -> {
                return Result(leftOperandValue < rightOperand, "$leftOperand >= $rightOperand")
            }

            OdrlIdentifiers.LTEQ -> {
                return Result(leftOperandValue <= rightOperand, "$leftOperand > $rightOperand")
            }

            OdrlIdentifiers.HASPART -> {
                if (leftOperandValue !is Collection<*>) {
                    throw OdrlException("Cannot evaluate $operator on left operand $leftOperandValue")
                }
                return Result(leftOperandValue.contains(rightOperand), "$leftOperand has no part of $rightOperand")
            }

            OdrlIdentifiers.ISA, OdrlIdentifiers.ISALLOF, OdrlIdentifiers.ISANYOF, OdrlIdentifiers.ISNONEOF -> {
                throw OdrlException("$operator is not supported")
            }
        }
        throw OdrlException("Invalid $operator")
    }
}
