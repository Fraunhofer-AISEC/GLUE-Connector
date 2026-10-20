/*
 * Copyright 2025 Fraunhofer AISEC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
    @param:JsonProperty("Id")
    val id: String? = null,
    // The names that this container has been given
    @param:JsonProperty("Names")
    val names: List<String>? = null,
    // The name of the image used when creating this container
    @param:JsonProperty("Image")
    val image: String? = null,
    // The ID of the image that this container was created from
    @param:JsonProperty("ImageID")
    val imageID: String? = null,
    // Command to run when starting the container
    @param:JsonProperty("Command")
    val command: String? = null,
    // When the container was created
    @param:JsonProperty("Created")
    val created: Long? = null,
    // The ports exposed by this container
    @param:JsonProperty("Ports")
    val ports: List<Port>? = null,
    // The size of files that have been created or changed by this container
    @param:JsonProperty("SizeRw")
    val sizeRw: Long? = null,
    // The total size of all the files in this container
    @param:JsonProperty("SizeRootFs")
    val sizeRootFs: Long? = null,
    // User-defined key/value metadata.
    @param:JsonProperty("Labels")
    val labels: Map<String, String>? = null,
    // The state of this container (e.g. `Exited`)
    @param:JsonProperty("State")
    val state: String? = null,
    // Additional human-readable status of this container (e.g. `Exit 0`)
    @param:JsonProperty("Status")
    val status: String? = null,
    @param:JsonProperty("HostConfig")
    val hostConfig: ContainerSummaryHostConfig? = null,
    @param:JsonProperty("NetworkSettings")
    val networkSettings: ContainerSummaryNetworkSettings? = null,
    @param:JsonProperty("Mounts")
    val mounts: List<MountPoint>? = null,
)
