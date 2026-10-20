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
 * Optional configuration for the `tmpfs` type.
 *
 * @param sizeBytes The size for the tmpfs mount in bytes.
 * @param mode The permission mode for the tmpfs mount in an integer.
 */
data class MountTmpfsOptions(
    // The size for the tmpfs mount in bytes.
    @param:JsonProperty("SizeBytes")
    val sizeBytes: Long? = null,
    // The permission mode for the tmpfs mount in an integer.
    @param:JsonProperty("Mode")
    val mode: Int? = null,
)
