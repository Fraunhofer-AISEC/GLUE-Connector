package de.fraunhofer.aisec.glue.docker.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Health stores information about the container's healthcheck results.
 *
 * @param status Status is one of `none`, `starting`, `healthy` or `unhealthy`  - \"none\"      Indicates there is no healthcheck - \"starting\"  Starting indicates that the container is not yet ready - \"healthy\"   Healthy indicates that the container is running correctly - \"unhealthy\" Unhealthy indicates that the container has a problem
 * @param failingStreak FailingStreak is the number of consecutive failures
 * @param log Log contains the last few results (oldest first)
 */
@Serializable
data class Health(
    // Status is one of `none`, `starting`, `healthy` or `unhealthy`  - \"none\"      Indicates there is no healthcheck - \"starting\"  Starting indicates that the container is not yet ready - \"healthy\"   Healthy indicates that the container is running correctly - \"unhealthy\" Unhealthy indicates that the container has a problem
    @SerialName(value = "Status")
    val status: String? = null,
    // FailingStreak is the number of consecutive failures
    @SerialName(value = "FailingStreak")
    val failingStreak: Int? = null,
    // Log contains the last few results (oldest first)
    @SerialName(value = "Log")
    val log: List<HealthcheckResult>? = null,
)
