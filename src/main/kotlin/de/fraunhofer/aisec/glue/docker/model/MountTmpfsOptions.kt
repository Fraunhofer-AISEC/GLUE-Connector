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
