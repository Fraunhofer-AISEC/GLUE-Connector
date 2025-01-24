package de.fraunhofer.aisec.glue.docker.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Additional metadata of the image in the local cache. This information is local to the daemon, and not part of the image itself.
 *
 * @param lastTagTime Date and time at which the image was last tagged in [RFC 3339](https://www.ietf.org/rfc/rfc3339.txt) format with nano-seconds.  This information is only available if the image was tagged locally, and omitted otherwise.
 */

data class ImageInspectMetadata(
    // Date and time at which the image was last tagged in [RFC 3339](https://www.ietf.org/rfc/rfc3339.txt) format with nano-seconds.  This information is only available if the image was tagged locally, and omitted otherwise.
    @get:JsonProperty("LastTagTime")
    val lastTagTime: kotlin.String? = null,
)
