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

import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.cancellation.CancellationException

/**
 * This class provides a simple, in-process event bus to deliver messages across subsystems and provide observability.
 * Each subsystem may define its own event type by implementing the [Event] interface.
 *
 * To subscribe to events, register a handler using the [subscribe] function.
 * Events submitted to the event bus using the [emit] function are delivered to all registered handlers sequentially and
 * on the calling coroutine.
 *
 * The [EventStream] class provides the same events asynchronously over a shared flow.
 */
class EventBus {
    private val handlers = AtomicReference<List<Entry>>(emptyList())

    /**
     * Register an event handler to receive events on this bus.
     *
     * @param priority Set the priority for this handler. Lower values mean higher priority. See [PRIORITY_HIGH],
     * [PRIORITY_NORMAL], and [PRIORITY_LOW] for reference.
     * @param throws If `true`, exceptions caught by the event handler are not handled by the event bus. This means that
     * the exception reaches the event emitter and further processing of handlers is halted.
     * @param filter The filter is called for every event to determine if it should be delivered to the registered
     * handler. As such, filter functions should be trivial. Exceptions thrown by the filter are treated like exceptions
     * thrown by the handler.
     * @param handler The event handler to be called on the emitting coroutine for each event that passes the filter.
     * @return A [Registration] that can be used to remove the handler when it is no longer required.
     *
     * @see on
     * @see once
     * @see EventStream
     */
    fun subscribe(
        priority: Int = PRIORITY_NORMAL,
        throws: Boolean = false,
        filter: (Event) -> Boolean = {
            true
        },
        handler: suspend (Event) -> Unit,
    ): Registration {
        val entry = Entry(priority, throws, filter, handler)
        handlers.updateAndGet { (it + entry).sorted() }
        return entry
    }

    private suspend fun emitOne(
        event: Event,
        entry: Entry,
    ) {
        if (entry.isCancelled.get()) {
            return
        }

        try {
            if (entry.filter(event)) {
                entry.handler(event)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LOG.atWarn {
                message = "Caught exception in event filter or handler"
                cause = e
                payload =
                    mapOf(
                        // Not sure how useful this is, but it is better than knowing nothing about the handler at all.
                        "filter" to entry.filter.javaClass.name,
                        "handler" to entry.handler.javaClass.name,
                        "event" to event,
                    )
            }

            if (entry.throws) {
                throw e
            }
        }
    }

    /**
     * Send an event to all registered handlers on the event bus.
     * Note that all events are processed sequentially and synchronously.
     *
     * @throws Exception Any exception that is thrown by an event handler registered with the `throws` option is
     * rethrown.
     */
    suspend fun emit(event: Event) {
        for (entry in handlers.get()) {
            emitOne(event, entry)
        }
    }

    /**
     * A reference to a registered event handler on the [EventBus]
     */
    fun interface Registration {
        /**
         * Removes the handler from the event bus.
         */
        fun cancel()
    }

    private inner class Entry(
        val priority: Int,
        val throws: Boolean,
        val filter: (Event) -> Boolean,
        val handler: suspend (Event) -> Unit,
    ) : Registration,
        Comparable<Entry> {
        val isCancelled = AtomicBoolean(false)

        override fun cancel() {
            if (isCancelled.compareAndSet(false, true)) {
                handlers.updateAndGet { it - this }
            }
        }

        override fun compareTo(other: Entry): Int = priority.compareTo(other.priority)
    }

    companion object {
        const val PRIORITY_HIGH = 0
        const val PRIORITY_NORMAL = 50
        const val PRIORITY_LOW = 100

        private val LOG = KotlinLogging.logger { }
    }
}
