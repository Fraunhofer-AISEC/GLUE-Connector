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
 * A test to perform to check that the container is healthy.
 *
 * @param test The test to perform. Possible values are:  - `[]` inherit healthcheck from image or parent image - `[\"NONE\"]` disable healthcheck - `[\"CMD\", args...]` exec arguments directly - `[\"CMD-SHELL\", command]` run command with system's default shell
 * @param interval The time to wait between checks in nanoseconds. It should be 0 or at least 1000000 (1 ms). 0 means inherit.
 * @param timeout The time to wait before considering the check to have hung. It should be 0 or at least 1000000 (1 ms). 0 means inherit.
 * @param retries The number of consecutive failures needed to consider a container as unhealthy. 0 means inherit.
 * @param startPeriod Start period for the container to initialize before starting health-retries countdown in nanoseconds. It should be 0 or at least 1000000 (1 ms). 0 means inherit.
 */
data class HealthConfig(
    // The test to perform. Possible values are:  - `[]` inherit healthcheck from image or parent image - `[\"NONE\"]` disable healthcheck - `[\"CMD\", args...]` exec arguments directly - `[\"CMD-SHELL\", command]` run command with system's default shell
    @param:JsonProperty("Test")
    val test: List<String>? = null,
    // The time to wait between checks in nanoseconds. It should be 0 or at least 1000000 (1 ms). 0 means inherit.
    @param:JsonProperty("Interval")
    val interval: Long? = null,
    // The time to wait before considering the check to have hung. It should be 0 or at least 1000000 (1 ms). 0 means inherit.
    @param:JsonProperty("Timeout")
    val timeout: Long? = null,
    // The number of consecutive failures needed to consider a container as unhealthy. 0 means inherit.
    @param:JsonProperty("Retries")
    val retries: Int? = null,
    // Start period for the container to initialize before starting health-retries countdown in nanoseconds. It should be 0 or at least 1000000 (1 ms). 0 means inherit.
    @param:JsonProperty("StartPeriod")
    val startPeriod: Long? = null,
)
