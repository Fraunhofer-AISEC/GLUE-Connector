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
package de.fraunhofer.aisec.glue.pipeline

data class ContainerPort(
    /**
     * The port inside the container that is forwarded to the host.
     */
    val containerPort: UShort,
    /**
     * The host:port pair where the container port is reachable on.
     */
    val hostAddress: String,
    /**
     * The protocol for the port.
     * Either `tcp` or `udp`.
     * Other protocols like `sctp` are currently not supported.
     */
    val protocol: String,
)
