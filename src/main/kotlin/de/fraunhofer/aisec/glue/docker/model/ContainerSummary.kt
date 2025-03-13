package de.fraunhofer.aisec.glue.docker.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * @param id The ID of this container
 * @param names The names that this container has been given
 * @param image The name of the image used when creating this container
 * @param imageID The ID of the image that this container was created from
 * @param command Command to run when starting the container
 * @param created When the container was created
 * @param ports The ports exposed by this container
 * @param sizeRw The size of files that have been created or changed by this container
 * @param sizeRootFs The total size of all the files in this container
 * @param labels User-defined key/value metadata.
 * @param state The state of this container (e.g. `Exited`)
 * @param status Additional human-readable status of this container (e.g. `Exit 0`)
 * @param hostConfig
 * @param networkSettings
 * @param mounts
 */

data class ContainerSummary(
    // The ID of this container
    @JsonProperty("Id")
    val id: String? = null,
    // The names that this container has been given
    @JsonProperty("Names")
    val names: List<String>? = null,
    // The name of the image used when creating this container
    @JsonProperty("Image")
    val image: String? = null,
    // The ID of the image that this container was created from
    @JsonProperty("ImageID")
    val imageID: String? = null,
    // Command to run when starting the container
    @JsonProperty("Command")
    val command: String? = null,
    // When the container was created
    @JsonProperty("Created")
    val created: Long? = null,
    // The ports exposed by this container
    @JsonProperty("Ports")
    val ports: List<Port>? = null,
    // The size of files that have been created or changed by this container
    @JsonProperty("SizeRw")
    val sizeRw: Long? = null,
    // The total size of all the files in this container
    @JsonProperty("SizeRootFs")
    val sizeRootFs: Long? = null,
    // User-defined key/value metadata.
    @JsonProperty("Labels")
    val labels: Map<String, String>? = null,
    // The state of this container (e.g. `Exited`)
    @JsonProperty("State")
    val state: String? = null,
    // Additional human-readable status of this container (e.g. `Exit 0`)
    @JsonProperty("Status")
    val status: String? = null,
    @JsonProperty("HostConfig")
    val hostConfig: ContainerSummaryHostConfig? = null,
    @JsonProperty("NetworkSettings")
    val networkSettings: ContainerSummaryNetworkSettings? = null,
    @JsonProperty("Mounts")
    val mounts: List<MountPoint>? = null,
)
