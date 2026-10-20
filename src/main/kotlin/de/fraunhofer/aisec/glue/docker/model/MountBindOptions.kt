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
 * Optional configuration for the `bind` type.
 *
 * @param propagation A propagation mode with the value `[r]private`, `[r]shared`, or `[r]slave`.
 * @param nonRecursive Disable recursive bind mount.
 * @param createMountpoint Create mount point on host if missing
 */
data class MountBindOptions(
    // A propagation mode with the value `[r]private`, `[r]shared`, or `[r]slave`.
    @param:JsonProperty("Propagation")
    val propagation: String? = null,
    // Disable recursive bind mount.
    @param:JsonProperty("NonRecursive")
    val nonRecursive: Boolean? = false,
    // Create mount point on host if missing
    @param:JsonProperty("CreateMountpoint")
    val createMountpoint: Boolean? = false,
)
