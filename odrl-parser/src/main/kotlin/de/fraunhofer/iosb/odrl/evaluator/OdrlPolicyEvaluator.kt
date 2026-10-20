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

import de.fraunhofer.iosb.odrl.OdrlException
import de.fraunhofer.iosb.odrl.OdrlPolicy

class OdrlPolicyEvaluator(
    private val permissionEvaluator: OdrlPermissionEvaluator = OdrlPermissionEvaluator(),
    private val prohibitionEvaluator: OdrlProhibitionEvaluator = OdrlProhibitionEvaluator(),
    private val dutyEvaluator: OdrlDutyEvaluator = OdrlDutyEvaluator(),
) {
    suspend fun evaluate(
        ctx: Context,
        policy: OdrlPolicy,
        actionCallback: OdrlActionCallback? = null,
        infringementCallback: OdrlInfringementCallback? = null,
    ): Decision {
        // Evaluate permissions
        val permissionResults = policy.permission.map { permissionEvaluator.isAllowed(ctx, it, actionCallback) }
        var allowed = permissionResults.any { it.success }

        // Evaluate prohibitions
        val prohibitionResults = policy.prohibition.map { prohibitionEvaluator.isDisallowed(ctx, it) }
        var disallowed = prohibitionResults.any { it.success }

        // Evaluate infringement violations
        val infringementViolations =
            policy.prohibition.flatMap {
                if (prohibitionEvaluator.isNotInfringed(ctx, it, actionCallback, infringementCallback).success) {
                    listOf()
                } else {
                    listOf(it.action)
                }
            }

        // Resolve conflicts
        if (allowed && disallowed) {
            if (policy.conflict == null || policy.conflict.isInvalid()) {
                throw OdrlException("Policy is conflicted: Cannot both prohibit and permit!")
            }
            if (policy.conflict.isPerm()) disallowed = false
            if (policy.conflict.isProhibit()) allowed = false
        }

        var result =
            if (!allowed) {
                Result(false, "No matching permission found. Individual results: $permissionResults")
            } else if (disallowed) {
                Result(false, "At least one prohibition found. Individual results: $prohibitionResults")
            } else {
                Result(true)
            }

        if (result.success) {
            // Exercise obligations
            val obligationResults = policy.obligation.map { dutyEvaluator.isFulfilled(ctx, it, actionCallback) }
            result =
                Result(
                    obligationResults.all { it.success },
                    "Not all obligations fulfilled. Individual results: $obligationResults",
                )
        }

        return Decision(result, infringementViolations)
    }
}
