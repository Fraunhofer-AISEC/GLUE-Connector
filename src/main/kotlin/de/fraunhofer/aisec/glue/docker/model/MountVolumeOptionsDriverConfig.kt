package de.fraunhofer.aisec.glue.docker.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Map of driver specific options
 *
 * @param name Name of the driver to use to create the volume.
 * @param options key/value map of driver specific options.
 */
@Serializable
data class MountVolumeOptionsDriverConfig(
    // Name of the driver to use to create the volume.
    @SerialName(value = "Name")
    val name: String? = null,
    // key/value map of driver specific options.
    @SerialName(value = "Options")
    val options: Map<String, String>? = null,
)
