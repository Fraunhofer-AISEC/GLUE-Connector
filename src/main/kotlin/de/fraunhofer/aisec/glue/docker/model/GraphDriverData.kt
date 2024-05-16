package de.fraunhofer.aisec.glue.docker.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Information about the storage driver used to store the container's and image's filesystem.
 *
 * @param name Name of the storage driver.
 * @param `data` Low-level storage metadata, provided as key/value pairs.  This information is driver-specific, and depends on the storage-driver in use, and should be used for informational purposes only.
 */
@Serializable
data class GraphDriverData(
    // Name of the storage driver.
    @SerialName(value = "Name")
    val name: String,
    // Low-level storage metadata, provided as key/value pairs.  This information is driver-specific, and depends on the storage-driver in use, and should be used for informational purposes only.
    @SerialName(value = "Data")
    val `data`: Map<String, String>,
)
