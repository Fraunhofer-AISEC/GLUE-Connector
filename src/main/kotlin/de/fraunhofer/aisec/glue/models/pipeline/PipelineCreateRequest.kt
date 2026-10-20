/*
 * Copyright 2024-2025 Fraunhofer AISEC
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
package de.fraunhofer.aisec.glue.models.pipeline

data class PipelineCreateRequest(
    // The target more or less identifies the pipeline to use instead of a data asset
    val target: String,
    // The contract id must be specified if there are more contracts referring to the target
    val contractId: String,
    // If there is more than one action in the selected contract, an action must be specified as well
    val action: String? = null,
)
