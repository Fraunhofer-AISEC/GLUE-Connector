package de.fraunhofer.aisec.glue.docker.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A test to perform to check that the container is healthy.
 *
 * @param test The test to perform. Possible values are:  - `[]` inherit healthcheck from image or parent image - `[\"NONE\"]` disable healthcheck - `[\"CMD\", args...]` exec arguments directly - `[\"CMD-SHELL\", command]` run command with system's default shell
 * @param interval The time to wait between checks in nanoseconds. It should be 0 or at least 1000000 (1 ms). 0 means inherit.
 * @param timeout The time to wait before considering the check to have hung. It should be 0 or at least 1000000 (1 ms). 0 means inherit.
 * @param retries The number of consecutive failures needed to consider a container as unhealthy. 0 means inherit.
 * @param startPeriod Start period for the container to initialize before starting health-retries countdown in nanoseconds. It should be 0 or at least 1000000 (1 ms). 0 means inherit.
 */
@Serializable
data class HealthConfig(
    // The test to perform. Possible values are:  - `[]` inherit healthcheck from image or parent image - `[\"NONE\"]` disable healthcheck - `[\"CMD\", args...]` exec arguments directly - `[\"CMD-SHELL\", command]` run command with system's default shell
    @SerialName(value = "Test")
    val test: List<String>? = null,
    // The time to wait between checks in nanoseconds. It should be 0 or at least 1000000 (1 ms). 0 means inherit.
    @SerialName(value = "Interval")
    val interval: Long? = null,
    // The time to wait before considering the check to have hung. It should be 0 or at least 1000000 (1 ms). 0 means inherit.
    @SerialName(value = "Timeout")
    val timeout: Long? = null,
    // The number of consecutive failures needed to consider a container as unhealthy. 0 means inherit.
    @SerialName(value = "Retries")
    val retries: Int? = null,
    // Start period for the container to initialize before starting health-retries countdown in nanoseconds. It should be 0 or at least 1000000 (1 ms). 0 means inherit.
    @SerialName(value = "StartPeriod")
    val startPeriod: Long? = null,
)
