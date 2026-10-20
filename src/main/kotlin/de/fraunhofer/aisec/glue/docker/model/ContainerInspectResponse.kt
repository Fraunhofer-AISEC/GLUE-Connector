/*
 * Copyright 2024-2025 Fraunhofer AISEC
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
 *
 *
 * @param id The ID of the container
 * @param created The time the container was created
 * @param path The path to the command being run
 * @param args The arguments to the command being run
 * @param state
 * @param image The container's image ID
 * @param resolvConfPath
 * @param hostnamePath
 * @param hostsPath
 * @param logPath
 * @param name
 * @param restartCount
 * @param driver
 * @param platform
 * @param mountLabel
 * @param processLabel
 * @param appArmorProfile
 * @param execIDs IDs of exec instances that are running in the container.
 * @param hostConfig
 * @param graphDriver
 * @param sizeRw The size of files that have been created or changed by this container.
 * @param sizeRootFs The total size of all the files in this container.
 * @param mounts
 * @param config
 * @param networkSettings
 */
data class ContainerInspectResponse(
    // The ID of the container
    @param:JsonProperty("Id")
    val id: String? = null,
    // The time the container was created
    @param:JsonProperty("Created")
    val created: String? = null,
    // The path to the command being run
    @param:JsonProperty("Path")
    val path: String? = null,
    // The arguments to the command being run
    @param:JsonProperty("Args")
    val args: List<String>? = null,
    @param:JsonProperty("State")
    val state: ContainerState? = null,
    // The container's image ID
    @param:JsonProperty("Image")
    val image: String? = null,
    @param:JsonProperty("ResolvConfPath")
    val resolvConfPath: String? = null,
    @param:JsonProperty("HostnamePath")
    val hostnamePath: String? = null,
    @param:JsonProperty("HostsPath")
    val hostsPath: String? = null,
    @param:JsonProperty("LogPath")
    val logPath: String? = null,
    @param:JsonProperty("Name")
    val name: String? = null,
    @param:JsonProperty("RestartCount")
    val restartCount: Int? = null,
    @param:JsonProperty("Driver")
    val driver: String? = null,
    @param:JsonProperty("Platform")
    val platform: String? = null,
    @param:JsonProperty("MountLabel")
    val mountLabel: String? = null,
    @param:JsonProperty("ProcessLabel")
    val processLabel: String? = null,
    @param:JsonProperty("AppArmorProfile")
    val appArmorProfile: String? = null,
    // IDs of exec instances that are running in the container.
    @param:JsonProperty("ExecIDs")
    val execIDs: List<String>? = null,
    @param:JsonProperty("HostConfig")
    val hostConfig: HostConfig? = null,
    @param:JsonProperty("GraphDriver")
    val graphDriver: GraphDriverData? = null,
    // The size of files that have been created or changed by this container.
    @param:JsonProperty("SizeRw")
    val sizeRw: Long? = null,
    // The total size of all the files in this container.
    @param:JsonProperty("SizeRootFs")
    val sizeRootFs: Long? = null,
    @param:JsonProperty("Mounts")
    val mounts: List<MountPoint>? = null,
    @param:JsonProperty("Config")
    val config: ContainerConfig? = null,
    @param:JsonProperty("NetworkSettings")
    val networkSettings: NetworkSettings? = null,
)
