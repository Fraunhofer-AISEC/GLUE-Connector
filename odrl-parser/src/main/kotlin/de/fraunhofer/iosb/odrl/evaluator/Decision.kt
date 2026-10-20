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

import de.fraunhofer.iosb.odrl.OdrlAction

data class Decision(
    val evaluationResult: Result,
    val infringementViolations: List<OdrlAction>,
) {
    override fun toString(): String {
        var string = if (evaluationResult.success) "ALLOW" else "DENY"
        if (infringementViolations.isNotEmpty()) {
            string += " with violations $infringementViolations"
        }
        return string
    }
}
