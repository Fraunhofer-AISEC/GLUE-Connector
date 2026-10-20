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

data class Network(
    @param:JsonProperty("Name")
    val name: String? = null,
    @param:JsonProperty("Id")
    val id: String? = null,
    @param:JsonProperty("Created")
    val created: String? = null,
    @param:JsonProperty("Scope")
    val scope: String? = null,
    @param:JsonProperty("Driver")
    val driver: String? = null,
    @param:JsonProperty("EnableIPv6")
    val enableIPv6: Boolean? = null,
    @param:JsonProperty("IPAM")
    val ipam: IPAM? = null,
    @param:JsonProperty("Internal")
    val `internal`: Boolean? = null,
    @param:JsonProperty("Attachable")
    val attachable: Boolean? = null,
    @param:JsonProperty("Ingress")
    val ingress: Boolean? = null,
    @param:JsonProperty("Containers")
    val containers: Map<String, NetworkContainer>? = null,
    @param:JsonProperty("Options")
    val options: Map<String, String>? = null,
    @param:JsonProperty("Labels")
    val labels: Map<String, String>? = null,
)
