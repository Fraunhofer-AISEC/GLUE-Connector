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
package de.fraunhofer.aisec.glue.negotiation

import de.fraunhofer.iosb.odrl.OdrlPolicy

abstract class NegotiationManager {
    /**
     * Return all currently ongoing negotiations.
     */
    abstract suspend fun getNegotiations(): Collection<Negotiation>

    /**
     * Get the negotiation with the provided ID.
     */
    abstract suspend fun getNegotiation(id: String): Negotiation?

    /**
     * Start a new negotiation with a peer connector and an arbitrary ODRL policy.
     * @param peer The URL or an internal ID for the peer to negotiate with. The format for internal IDs is
     * implementation defined.
     * @param offer The contract to propose to the peer. If this contract is not an ODRL offer, it is converted to one
     * using the current connector's identity.
     * @return The newly created negotiation object.
     */
    abstract suspend fun startNegotiation(
        peer: String,
        offer: OdrlPolicy,
    ): Negotiation
}
