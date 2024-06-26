package de.fraunhofer.aisec.glue.docker.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Optional configuration for the `volume` type.
 *
 * @param noCopy Populate volume with data from the target.
 * @param labels User-defined key/value metadata.
 * @param driverConfig
 */
data class MountVolumeOptions(
    // Populate volume with data from the target.
    @JsonProperty("NoCopy")
    val noCopy: Boolean? = false,
    // User-defined key/value metadata.
    @JsonProperty("Labels")
    val labels: Map<String, String>? = null,
    @JsonProperty("DriverConfig")
    val driverConfig: MountVolumeOptionsDriverConfig? = null,
)
