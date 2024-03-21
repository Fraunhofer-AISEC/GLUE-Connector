package de.fraunhofer.aisec.glue.docker.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Optional configuration for the `volume` type.
 *
 * @param noCopy Populate volume with data from the target.
 * @param labels User-defined key/value metadata.
 * @param driverConfig
 */
@Serializable
data class MountVolumeOptions(
    // Populate volume with data from the target.
    @SerialName(value = "NoCopy")
    val noCopy: Boolean? = false,
    // User-defined key/value metadata.
    @SerialName(value = "Labels")
    val labels: Map<String, String>? = null,
    @SerialName(value = "DriverConfig")
    val driverConfig: MountVolumeOptionsDriverConfig? = null,
)
