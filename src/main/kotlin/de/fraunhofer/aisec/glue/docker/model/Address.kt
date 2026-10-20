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
 * Address represents an IPv4 or IPv6 IP address.
 *
 * @param addr IP address.
 * @param prefixLen Mask length of the IP address.
 */
data class Address(
    // IP address.
    @param:JsonProperty("Addr")
    val addr: String? = null,
    // Mask length of the IP address.
    @param:JsonProperty("PrefixLen")
    val prefixLen: Int? = null,
)
