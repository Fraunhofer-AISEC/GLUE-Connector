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
    @JsonProperty("Propagation")
    val propagation: String? = null,
    // Disable recursive bind mount.
    @JsonProperty("NonRecursive")
    val nonRecursive: Boolean? = false,
    // Create mount point on host if missing
    @JsonProperty("CreateMountpoint")
    val createMountpoint: Boolean? = false,
)
