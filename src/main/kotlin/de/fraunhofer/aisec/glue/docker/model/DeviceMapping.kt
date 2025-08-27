package de.fraunhofer.aisec.glue.docker.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * A device mapping between the host and container
 *
 * @param pathOnHost
 * @param pathInContainer
 * @param cgroupPermissions
 */
data class DeviceMapping(
    @param:JsonProperty("PathOnHost")
    val pathOnHost: String? = null,
    @param:JsonProperty("PathInContainer")
    val pathInContainer: String? = null,
    @param:JsonProperty("CgroupPermissions")
    val cgroupPermissions: String? = null,
)
