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
package de.fraunhofer.aisec.glue.negotiation.builtin

import de.fraunhofer.aisec.glue.events.Event
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Emitted once for every newly created negotiation.
 */
data class NegotiationCreatedEvent(
    val data: BuiltinNegotiationData,
    override val timestamp: Instant = Clock.System.now(),
) : Event {
    override val action = "CREATED"
    override val source = data.id.toString()
}

/**
 * Emitted for every state update of an ongoing negotiation.
 */
data class NegotiationUpdatedEvent(
    val data: BuiltinNegotiationData,
    override val timestamp: Instant = Clock.System.now(),
) : Event {
    override val action = data.state.toString()
    override val source = data.id.toString()
}
