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

data class NetworkCreateRequest(
    // The network's name.
    @param:JsonProperty("Name")
    val name: String,
    // Check for networks with duplicate names. Since Network is primarily keyed based on a random ID and not on the name, and network name is strictly a user-friendly alias to the network which is uniquely identified using ID, there is no guaranteed way to check for duplicates. CheckDuplicate is there to provide a best effort checking of any networks which has the same name but it is not guaranteed to catch all name collisions.
    @param:JsonProperty("CheckDuplicate")
    val checkDuplicate: Boolean? = null,
    // Name of the network driver plugin to use.
    @param:JsonProperty("Driver")
    val driver: String? = "bridge",
    // Restrict external access to the network.
    @param:JsonProperty("Internal")
    val `internal`: Boolean? = null,
    // Globally scoped network is manually attachable by regular containers from workers in swarm mode.
    @param:JsonProperty("Attachable")
    val attachable: Boolean? = null,
    // Ingress network is the network which provides the routing-mesh in swarm mode.
    @param:JsonProperty("Ingress")
    val ingress: Boolean? = null,
    @param:JsonProperty("IPAM")
    val ipam: IPAM? = null,
    // Enable IPv6 on the network.
    @param:JsonProperty("EnableIPv6")
    val enableIPv6: Boolean? = null,
    // Network specific options to be used by the drivers.
    @param:JsonProperty("Options")
    val options: Map<String, String>? = null,
    // User-defined key/value metadata.
    @param:JsonProperty("Labels")
    val labels: Map<String, String>? = null,
)
