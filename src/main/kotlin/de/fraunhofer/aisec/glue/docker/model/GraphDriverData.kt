package de.fraunhofer.aisec.glue.docker.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Information about the storage driver used to store the container's and image's filesystem.
 *
 * @param name Name of the storage driver.
 * @param `data` Low-level storage metadata, provided as key/value pairs.  This information is driver-specific, and depends on the storage-driver in use, and should be used for informational purposes only.
 */
data class GraphDriverData(
    // Name of the storage driver.
    @JsonProperty("Name")
    val name: String,
    // Low-level storage metadata, provided as key/value pairs.  This information is driver-specific, and depends on the storage-driver in use, and should be used for informational purposes only.
    @JsonProperty("Data")
    val `data`: Map<String, String>,
)
