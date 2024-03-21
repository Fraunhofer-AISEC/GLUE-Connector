package de.fraunhofer.aisec.glue.docker.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param target Container path.
 * @param source Mount source (e.g. a volume name, a host path).
 * @param type The mount type. Available types:  - `bind` Mounts a file or directory from the host into the container. Must exist prior to creating the container. - `volume` Creates a volume with the given name and options (or uses a pre-existing volume with the same name and options). These are **not** removed when the container is removed. - `tmpfs` Create a tmpfs with the given options. The mount source cannot be specified for tmpfs. - `npipe` Mounts a named pipe from the host into the container. Must exist prior to creating the container. - `cluster` a Swarm cluster volume
 * @param readOnly Whether the mount should be read-only.
 * @param consistency The consistency requirement for the mount: `default`, `consistent`, `cached`, or `delegated`.
 * @param bindOptions
 * @param volumeOptions
 * @param tmpfsOptions
 */
@Serializable
data class Mount(
    // Container path.
    @SerialName(value = "Target")
    val target: String? = null,
    // Mount source (e.g. a volume name, a host path).
    @SerialName(value = "Source")
    val source: String? = null,
    // The mount type. Available types:  - `bind` Mounts a file or directory from the host into the container. Must exist prior to creating the container. - `volume` Creates a volume with the given name and options (or uses a pre-existing volume with the same name and options). These are **not** removed when the container is removed. - `tmpfs` Create a tmpfs with the given options. The mount source cannot be specified for tmpfs. - `npipe` Mounts a named pipe from the host into the container. Must exist prior to creating the container. - `cluster` a Swarm cluster volume
    @SerialName(value = "Type")
    val type: String? = null,
    // Whether the mount should be read-only.
    @SerialName(value = "ReadOnly")
    val readOnly: Boolean? = null,
    // The consistency requirement for the mount: `default`, `consistent`, `cached`, or `delegated`.
    @SerialName(value = "Consistency")
    val consistency: String? = null,
    @SerialName(value = "BindOptions")
    val bindOptions: MountBindOptions? = null,
    @SerialName(value = "VolumeOptions")
    val volumeOptions: MountVolumeOptions? = null,
    @SerialName(value = "TmpfsOptions")
    val tmpfsOptions: MountTmpfsOptions? = null,
)
