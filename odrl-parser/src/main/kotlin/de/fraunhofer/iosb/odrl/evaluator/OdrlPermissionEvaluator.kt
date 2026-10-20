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
import de.fraunhofer.iosb.odrl.OdrlException
import de.fraunhofer.iosb.odrl.OdrlLogicalConstraint
import de.fraunhofer.iosb.odrl.OdrlPermission

class OdrlPermissionEvaluator(
    private val actionEvaluator: OdrlActionEvaluator = OdrlActionEvaluator(),
    private val assetEvaluator: OdrlAssetEvaluator = OdrlAssetEvaluator(),
    private val constraintEvaluator: OdrlConstraintEvaluator = OdrlConstraintEvaluator(),
    private val dutyEvaluator: OdrlDutyEvaluator = OdrlDutyEvaluator(),
) {
    suspend fun isAllowed(
        ctx: Context,
        permission: OdrlPermission,
        actionCallback: OdrlActionCallback? = null,
    ): Result {
        // Action has to match
        var result = actionEvaluator.matches(ctx, permission.action)
        if (!result.success) {
            return Result(result, "${permission.action} does not match")
        }

        // Target has to match
        if (permission.target != null) {
            result = assetEvaluator.matches(ctx, permission.target)
            if (!result.success) {
                return Result(result, "${permission.target} does not match")
            }
        } else {
            throw OdrlException("Invalid $permission: Expected property 'target'")
        }

        // Constraints have to be satisfied
        for (c in permission.constraint) {
            result =
                when (c) {
                    is OdrlConstraint -> constraintEvaluator.isSatisfied(ctx, c)
                    is OdrlLogicalConstraint -> constraintEvaluator.isSatisfied(ctx, c)
                    else -> throw OdrlException("Invalid $c: Expected type 'OdrlConstraint' or 'OdrlLogicalConstraint'")
                }
            if (!result.success) {
                return Result(result, "$c is not satisfied")
            }
        }

        // Duties have to be fulfilled
        for (d in permission.duty) {
            result = dutyEvaluator.isFulfilled(ctx, d, actionCallback)
            if (!result.success) {
                return Result(result, "$d is not fulfilled")
            }
        }

        return Result(true) // This permission allows context |ctx|
    }
}
