package de.fraunhofer.aisec.glue.docker.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Map of driver specific options
 *
 * @param name Name of the driver to use to create the volume.
 * @param options key/value map of driver specific options.
 */
data class MountVolumeOptionsDriverConfig(
    // Name of the driver to use to create the volume.
    @JsonProperty("Name")
    val name: String? = null,
    // key/value map of driver specific options.
    @JsonProperty("Options")
    val options: Map<String, String>? = null,
)
