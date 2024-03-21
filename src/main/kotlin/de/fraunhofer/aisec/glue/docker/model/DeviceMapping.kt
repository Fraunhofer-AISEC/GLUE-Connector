package de.fraunhofer.aisec.glue.docker.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A device mapping between the host and container
 *
 * @param pathOnHost
 * @param pathInContainer
 * @param cgroupPermissions
 */
@Serializable
data class DeviceMapping(
    @SerialName(value = "PathOnHost")
    val pathOnHost: String? = null,
    @SerialName(value = "PathInContainer")
    val pathInContainer: String? = null,
    @SerialName(value = "CgroupPermissions")
    val cgroupPermissions: String? = null,
)
