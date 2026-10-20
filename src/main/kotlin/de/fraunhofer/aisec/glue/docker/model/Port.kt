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
 * An open port on a container
 *
 * @param privatePort Port on the container
 * @param type
 * @param ip Host IP address that the container's port is mapped to
 * @param publicPort Port exposed on the host
 */
data class Port(
    // Port on the container
    @param:JsonProperty("PrivatePort")
    val privatePort: Int,
    @param:JsonProperty("Type")
    val type: Type,
    // Host IP address that the container's port is mapped to
    @param:JsonProperty("IP")
    val ip: String? = null,
    // Port exposed on the host
    @param:JsonProperty("PublicPort")
    val publicPort: Int? = null,
) {
    enum class Type(
        val value: String,
    ) {
        @JsonProperty("tcp")
        TCP("tcp"),

        @JsonProperty("udp")
        UDP("udp"),

        @JsonProperty("sctp")
        SCTP("sctp"),
    }
}
