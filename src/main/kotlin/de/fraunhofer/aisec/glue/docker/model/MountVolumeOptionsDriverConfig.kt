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
package de.fraunhofer.aisec.glue.docker.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Map of driver specific options
 *
 * @param name Name of the driver to use to create the volume.
 * @param options key/value map of driver specific options.
 */
data class MountVolumeOptionsDriverConfig(
    // Name of the driver to use to create the volume.
    @param:JsonProperty("Name")
    val name: String? = null,
    // key/value map of driver specific options.
    @param:JsonProperty("Options")
    val options: Map<String, String>? = null,
)
