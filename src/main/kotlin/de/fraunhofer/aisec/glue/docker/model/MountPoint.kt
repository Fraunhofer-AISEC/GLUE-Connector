package de.fraunhofer.aisec.glue.docker.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * MountPoint represents a mount point configuration inside the container. This is used for reporting the mountpoints in use by a container.
 *
 * @param type The mount type:  - `bind` a mount of a file or directory from the host into the container. - `volume` a docker volume with the given `Name`. - `tmpfs` a `tmpfs`. - `npipe` a named pipe from the host into the container. - `cluster` a Swarm cluster volume
 * @param name Name is the name reference to the underlying data defined by `Source` e.g., the volume name.
 * @param source Source location of the mount.  For volumes, this contains the storage location of the volume (within `/var/lib/docker/volumes/`). For bind-mounts, and `npipe`, this contains the source (host) part of the bind-mount. For `tmpfs` mount points, this field is empty.
 * @param destination Destination is the path relative to the container root (`/`) where the `Source` is mounted inside the container.
 * @param driver Driver is the volume driver used to create the volume (if it is a volume).
 * @param mode Mode is a comma separated list of options supplied by the user when creating the bind/volume mount.  The default is platform-specific (`\"z\"` on Linux, empty on Windows).
 * @param RW Whether the mount is mounted writable (read-write).
 * @param propagation Propagation describes how mounts are propagated from the host into the mount point, and vice-versa. Refer to the [Linux kernel documentation](https://www.kernel.org/doc/Documentation/filesystems/sharedsubtree.txt) for details. This field is not used on Windows.
 */
@Serializable
data class MountPoint(
    // The mount type:  - `bind` a mount of a file or directory from the host into the container. - `volume` a docker volume with the given `Name`. - `tmpfs` a `tmpfs`. - `npipe` a named pipe from the host into the container. - `cluster` a Swarm cluster volume
    @SerialName(value = "Type")
    val type: String? = null,
    // Name is the name reference to the underlying data defined by `Source` e.g., the volume name.
    @SerialName(value = "Name")
    val name: String? = null,
    // Source location of the mount.  For volumes, this contains the storage location of the volume (within `/var/lib/docker/volumes/`). For bind-mounts, and `npipe`, this contains the source (host) part of the bind-mount. For `tmpfs` mount points, this field is empty.
    @SerialName(value = "Source")
    val source: String? = null,
    // Destination is the path relative to the container root (`/`) where the `Source` is mounted inside the container.
    @SerialName(value = "Destination")
    val destination: String? = null,
    // Driver is the volume driver used to create the volume (if it is a volume).
    @SerialName(value = "Driver")
    val driver: String? = null,
    // Mode is a comma separated list of options supplied by the user when creating the bind/volume mount.  The default is platform-specific (`\"z\"` on Linux, empty on Windows).
    @SerialName(value = "Mode")
    val mode: String? = null,
    // Whether the mount is mounted writable (read-write).
    @SerialName(value = "RW")
    val rw: Boolean? = null,
    // Propagation describes how mounts are propagated from the host into the mount point, and vice-versa. Refer to the [Linux kernel documentation](https://www.kernel.org/doc/Documentation/filesystems/sharedsubtree.txt) for details. This field is not used on Windows.
    @SerialName(value = "Propagation")
    val propagation: String? = null,
)
