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

import de.fraunhofer.aisec.glue.events.EventBus.Companion.PRIORITY_NORMAL
import de.fraunhofer.aisec.glue.events.EventBus.Registration
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Register an event handler on the event bus for events of type [T] only.
 * This function is a convenience wrapper around [EventBus.subscribe].
 */
inline fun <reified T : Event> EventBus.on(
    priority: Int = PRIORITY_NORMAL,
    throws: Boolean = false,
    crossinline filter: (T) -> Boolean = {
        true
    },
    crossinline handler: suspend (T) -> Unit,
): Registration = subscribe(priority, throws, { it is T && filter(it) }, { handler(it as T) })

/**
 * Register an event handler to run once for an event of type [T] that matches the filter.
 * Afterward, the handler is deregistered automatically.
 */
inline fun <reified T : Event> EventBus.once(
    priority: Int = PRIORITY_NORMAL,
    throws: Boolean = false,
    crossinline filter: (T) -> Boolean = {
        true
    },
    crossinline handler: suspend (T) -> Unit,
): Registration {
    val done = AtomicBoolean(false)
    val self = AtomicReference<Registration?>(null)
    val subscription =
        on<T>(priority, throws, filter) {
            if (done.compareAndSet(false, true)) {
                try {
                    handler(it)
                } finally {
                    self.get()?.cancel()
                }
            }
        }
    self.set(subscription)
    // An event may have already been delivered before the reference was set.
    if (done.get()) {
        subscription.cancel()
    }
    return Registration {
        done.compareAndSet(false, true).also { if (it) subscription.cancel() }
    }
}
