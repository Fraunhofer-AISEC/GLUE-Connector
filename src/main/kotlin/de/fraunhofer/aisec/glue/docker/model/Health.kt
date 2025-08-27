package de.fraunhofer.aisec.glue.docker.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Health stores information about the container's healthcheck results.
 *
 * @param status Status is one of `none`, `starting`, `healthy` or `unhealthy`  - \"none\"      Indicates there is no healthcheck - \"starting\"  Starting indicates that the container is not yet ready - \"healthy\"   Healthy indicates that the container is running correctly - \"unhealthy\" Unhealthy indicates that the container has a problem
 * @param failingStreak FailingStreak is the number of consecutive failures
 * @param log Log contains the last few results (oldest first)
 */
data class Health(
    // Status is one of `none`, `starting`, `healthy` or `unhealthy`  - \"none\"      Indicates there is no healthcheck - \"starting\"  Starting indicates that the container is not yet ready - \"healthy\"   Healthy indicates that the container is running correctly - \"unhealthy\" Unhealthy indicates that the container has a problem
    @param:JsonProperty("Status")
    val status: String? = null,
    // FailingStreak is the number of consecutive failures
    @param:JsonProperty("FailingStreak")
    val failingStreak: Int? = null,
    // Log contains the last few results (oldest first)
    @param:JsonProperty("Log")
    val log: List<HealthcheckResult>? = null,
)
