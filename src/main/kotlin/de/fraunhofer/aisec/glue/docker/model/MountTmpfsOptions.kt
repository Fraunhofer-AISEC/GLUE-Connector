package de.fraunhofer.aisec.glue.docker.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Optional configuration for the `tmpfs` type.
 *
 * @param sizeBytes The size for the tmpfs mount in bytes.
 * @param mode The permission mode for the tmpfs mount in an integer.
 */
@Serializable
data class MountTmpfsOptions(
    // The size for the tmpfs mount in bytes.
    @SerialName(value = "SizeBytes")
    val sizeBytes: Long? = null,
    // The permission mode for the tmpfs mount in an integer.
    @SerialName(value = "Mode")
    val mode: Int? = null,
)
