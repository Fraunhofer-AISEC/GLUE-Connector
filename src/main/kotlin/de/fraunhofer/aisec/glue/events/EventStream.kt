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
package de.fraunhofer.aisec.glue.events

import de.fraunhofer.aisec.glue.config.Config
import de.fraunhofer.aisec.glue.config.ConfigManager
import io.ktor.server.config.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Instant

/**
 * This class provides an asynchronous flow of the events delivered on the [EventBus].
 * The most recent events are retained as history up to a configurable queue size defaulting to 2048 events.
 * The same buffer is used for slow collectors. When it is full, older events are discarded.
 */
class EventStream(
    eventBus: EventBus,
    configManager: ConfigManager,
) {
    private val eventsConfig = configManager.getConfig(::EventsConfig)

    // Each event is numbered so collectors can tell events retained from before their subscription from new events.
    private val history: MutableSharedFlow<Pair<Long, Event>>
    private val sequence = AtomicLong()

    init {
        // The upper bound would definitely overflow memory, but thats on the admin at this point.
        require(eventsConfig.queueSize >= 1) {
            "queueSize must be larger than one"
        }
        history =
            MutableSharedFlow(
                replay = eventsConfig.queueSize,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )
        // With DROP_OLDEST, tryEmit never fails or suspends.
        eventBus.subscribe(EventBus.PRIORITY_NORMAL - 10) { history.tryEmit(sequence.incrementAndGet() to it) }
    }

    /**
     * Get a flow of all events emitted after collection starts.
     *
     * @param since If set to a non-null value, past events starting at the provided time stamp are replayed. Note that
     * the number of events that are retained is limited by the `events.queueSize` config option, meaning that delivery
     * of all past events cannot be guaranteed.
     *
     * @return A **cold** flow of events.
     */
    fun events(since: Instant? = null): Flow<Event> =
        flow {
            val start = sequence.get()
            emitAll(
                history
                    .filter { (seq, event) -> seq > start || (since != null && event.timestamp >= since) }
                    .map { it.second },
            )
        }

    class EventsConfig(
        raw: ApplicationConfig,
    ) : Config(raw) {
        val queueSize by withDefault(intProp(), 2048)
    }
}
