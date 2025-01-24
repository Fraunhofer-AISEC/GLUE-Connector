package de.fraunhofer.aisec.glue.docker.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Information about the image's RootFS, including the layer IDs.
 *
 * @param type
 * @param layers
 */

data class ImageInspectRootFS(
    @get:JsonProperty("Type")
    val type: kotlin.String,
    @get:JsonProperty("Layers")
    val layers: kotlin.collections.List<kotlin.String>? = null,
)
