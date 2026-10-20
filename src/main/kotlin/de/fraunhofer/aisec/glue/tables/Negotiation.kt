/*
 * Copyright 2026 Fraunhofer AISEC
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
package de.fraunhofer.aisec.glue.tables

import de.fraunhofer.aisec.glue.models.negotiation.NegotiationState
import de.fraunhofer.aisec.glue.negotiation.Negotiation.Role
import de.fraunhofer.iosb.odrl.OdrlPolicy
import org.ktorm.entity.Entity
import java.util.UUID
import kotlin.time.Instant

interface Negotiation : Entity<Negotiation> {
    companion object : Entity.Factory<Negotiation>()

    val id: UUID
    val role: Role
    val peer: String?
    val peerPid: String
    val startTimestamp: Instant
    val state: NegotiationState
    val callbackUrl: String
    val peerUrl: String
    val contract: OdrlPolicy?
    val failureReason: String?
    val lastReceivedMessage: String?
    val nextSentMessage: Int
}
