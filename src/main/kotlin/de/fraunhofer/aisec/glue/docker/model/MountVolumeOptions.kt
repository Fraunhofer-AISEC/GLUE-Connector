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
 * Optional configuration for the `volume` type.
 *
 * @param noCopy Populate volume with data from the target.
 * @param labels User-defined key/value metadata.
 * @param driverConfig
 */
data class MountVolumeOptions(
    // Populate volume with data from the target.
    @param:JsonProperty("NoCopy")
    val noCopy: Boolean? = false,
    // User-defined key/value metadata.
    @param:JsonProperty("Labels")
    val labels: Map<String, String>? = null,
    @param:JsonProperty("DriverConfig")
    val driverConfig: MountVolumeOptionsDriverConfig? = null,
)
