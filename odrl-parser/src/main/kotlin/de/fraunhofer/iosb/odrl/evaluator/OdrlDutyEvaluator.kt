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

import de.fraunhofer.iosb.odrl.OdrlConstraint
import de.fraunhofer.iosb.odrl.OdrlDuty
import de.fraunhofer.iosb.odrl.OdrlException
import de.fraunhofer.iosb.odrl.OdrlLogicalConstraint

class OdrlDutyEvaluator(
    private val actionEvaluator: OdrlActionEvaluator = OdrlActionEvaluator(),
    private val assetEvaluator: OdrlAssetEvaluator = OdrlAssetEvaluator(),
    private val constraintEvaluator: OdrlConstraintEvaluator = OdrlConstraintEvaluator(),
) {
    suspend fun isFulfilled(
        ctx: Context,
        duty: OdrlDuty,
        actionCallback: OdrlActionCallback? = null,
    ): Result {
        var result: Result

        // If target does not match, mark duty as fulfilled
        if (duty.target != null) {
            if (!assetEvaluator.matches(ctx, duty.target).success) {
                return Result(true)
            }
        }

        // If not all constraints are satisfied, mark duty as fulfilled
        for (c in duty.constraint) {
            result =
                when (c) {
                    is OdrlConstraint -> constraintEvaluator.isSatisfied(ctx, c)
                    is OdrlLogicalConstraint -> constraintEvaluator.isSatisfied(ctx, c)
                    else -> throw OdrlException("Invalid $c: Expected type 'OdrlConstraint' or 'OdrlLogicalConstraint'")
                }
            if (!result.success) {
                return Result(true)
            }
        }

        // Action must be exercised
        result = actionEvaluator.exercise(ctx, duty.action, actionCallback)
        if (!result.success) {
            result = Result(result, "${duty.action} was not exercised successfully")

            // If action has not been successfully exercised, consequences have to be fulfilled
            val results = duty.consequence.map { isFulfilled(ctx, it, actionCallback) }
            if (results.any { !it.success }) {
                return Result(result, "Not all consequences fulfilled. Individual results: $results")
            }
            return result
        }

        return Result(true)
    }
}
