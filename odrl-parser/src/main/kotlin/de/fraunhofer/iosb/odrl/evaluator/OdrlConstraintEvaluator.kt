/*
 * Copyright 2024 Fraunhofer AISEC and Fraunhofer IOSB
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

import de.fraunhofer.iosb.odrl.OdrlConstraint
import de.fraunhofer.iosb.odrl.OdrlException
import de.fraunhofer.iosb.odrl.OdrlLogicalConstraint
import java.util.function.Predicate

class OdrlConstraintEvaluator(
    private val operatorEvaluator: OdrlOperatorEvaluator = OdrlOperatorEvaluator(),
) {
    fun isSatisfied(
        ctx: Context,
        logicalConstraint: OdrlLogicalConstraint,
    ): Result =
        if (logicalConstraint.or.isNotEmpty()) {
            isConstraintListSatisfied(ctx, logicalConstraint.or) { results -> results.any { it.success } }
        } else if (logicalConstraint.xone.isNotEmpty()) {
            isConstraintListSatisfied(ctx, logicalConstraint.xone) { results -> results.count { it.success } == 1 }
        } else if (logicalConstraint.and.isNotEmpty()) {
            isConstraintListSatisfied(ctx, logicalConstraint.and) { results -> results.all { it.success } }
        } else if (logicalConstraint.andSequence.isNotEmpty()) {
            isConstraintListSatisfied(ctx, logicalConstraint.andSequence) { results -> results.all { it.success } }
        } else {
            throw OdrlException("Invalid $logicalConstraint: Expected property 'or', 'xone', 'and', or 'andSequence'")
        }

    private fun isConstraintListSatisfied(
        ctx: Context,
        constraintList: List<OdrlConstraint>,
        predicate: Predicate<List<Result>>,
    ): Result {
        val results = constraintList.map { isSatisfied(ctx, it) }
        if (predicate.test(results)) {
            return Result(true)
        }
        return Result(false, "Not all constraints satisfied. Individual results: $results")
    }

    fun isSatisfied(
        ctx: Context,
        constraint: OdrlConstraint,
    ): Result =
        if (constraint.rightOperand.isNotEmpty()) {
            operatorEvaluator.evaluateWithRightOperand(ctx, constraint.operator, constraint.leftOperand, constraint.rightOperand)
        } else if (constraint.rightOperandReference.isNotEmpty()) {
            operatorEvaluator.evaluateWithRightOperandReference(
                ctx,
                constraint.operator,
                constraint.leftOperand,
                constraint.rightOperandReference,
            )
        } else {
            throw OdrlException("Invalid $constraint: Expected property 'rightOperand' or 'rightOperandReference'")
        }
}
