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
 * A device mapping between the host and container
 *
 * @param pathOnHost
 * @param pathInContainer
 * @param cgroupPermissions
 */
data class DeviceMapping(
    @param:JsonProperty("PathOnHost")
    val pathOnHost: String? = null,
    @param:JsonProperty("PathInContainer")
    val pathInContainer: String? = null,
    @param:JsonProperty("CgroupPermissions")
    val cgroupPermissions: String? = null,
)
