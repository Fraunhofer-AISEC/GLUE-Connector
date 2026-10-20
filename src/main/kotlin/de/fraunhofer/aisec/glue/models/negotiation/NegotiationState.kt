/*
 * Copyright 2024-2026 Fraunhofer AISEC
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
package de.fraunhofer.aisec.glue.models.negotiation

import com.fasterxml.jackson.annotation.JsonProperty

enum class NegotiationState {
    /**
     * Note: The INITIAL state may never be sent to a peer
     */
    INITIAL,

    @JsonProperty("${Namespaces.DSPACE}/REQUESTED")
    REQUESTED,

    @JsonProperty("${Namespaces.DSPACE}/OFFERED")
    OFFERED,

    @JsonProperty("${Namespaces.DSPACE}/ACCEPTED")
    ACCEPTED,

    @JsonProperty("${Namespaces.DSPACE}/AGREED")
    AGREED,

    @JsonProperty("${Namespaces.DSPACE}/VERIFIED")
    VERIFIED,

    @JsonProperty("${Namespaces.DSPACE}/FINALIZED")
    FINALIZED,

    @JsonProperty("${Namespaces.DSPACE}/TERMINATED")
    TERMINATED,
}
