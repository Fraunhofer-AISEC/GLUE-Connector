package de.fraunhofer.aisec.glue.docker.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Optional configuration for the `bind` type.
 *
 * @param propagation A propagation mode with the value `[r]private`, `[r]shared`, or `[r]slave`.
 * @param nonRecursive Disable recursive bind mount.
 * @param createMountpoint Create mount point on host if missing
 */
@Serializable
data class MountBindOptions(
    // A propagation mode with the value `[r]private`, `[r]shared`, or `[r]slave`.
    @SerialName(value = "Propagation")
    val propagation: String? = null,
    // Disable recursive bind mount.
    @SerialName(value = "NonRecursive")
    val nonRecursive: Boolean? = false,
    // Create mount point on host if missing
    @SerialName(value = "CreateMountpoint")
    val createMountpoint: Boolean? = false,
)
