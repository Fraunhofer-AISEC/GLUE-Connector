/*
 * Copyright 2024 Fraunhofer AISEC
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
package de.fraunhofer.aisec.glue.pipeline

import de.fraunhofer.iosb.odrl.glue.GlueParameter

/**
 * The configuration object that is injected into every pipeline container at `/config.json`.
 */
data class ContainerConfig(
    /**
     * The address the controller can be reached on.
     */
    val controller: String,
    /**
     * The address or hostname the next container is reachable on or null if this is the last container in the pipeline.
     * For now, this is just the name of the container.
     */
    val nextContainer: String?,
    /**
     * The values of all arguments from the policy as specified by its parameters.
     * Parameters will be identified by their full ODRL/JsonLD types with the namespace expanded.
     */
    val parameters: List<GlueParameter>,
    //       We probably don't need to include the parameters to expand since it should know that already
)
