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

data class Result(
    val success: Boolean,
    val failureReasoning: MutableList<String> = mutableListOf(),
) {
    init {
        if (success && failureReasoning.isNotEmpty()) {
            throw IllegalArgumentException("Successful result cannot have failure reasons")
        }
    }

    constructor(
        success: Boolean,
        failureReason: String,
    ) : this(success, if (success) mutableListOf() else mutableListOf(failureReason))

    constructor(
        result: Result,
        failureReason: String,
    ) : this(result.success, result.failureReasoning) {
        if (!this.success) {
            this.failureReasoning.add(0, failureReason)
        }
    }
}
