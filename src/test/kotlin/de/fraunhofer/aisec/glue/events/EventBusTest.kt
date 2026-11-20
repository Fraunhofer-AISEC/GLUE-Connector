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

import de.fraunhofer.aisec.glue.config.ConfigManager
import io.ktor.server.config.*
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class EventBusTest {
    private data class TestEvent(
        override val action: String,
        override val timestamp: Instant = Instant.fromEpochMilliseconds(0),
        override val source: String = "test",
    ) : Event

    private data class OtherEvent(
        override val action: String,
        override val timestamp: Instant = Instant.fromEpochMilliseconds(0),
        override val source: String = "other",
    ) : Event

    @Test
    fun `Handlers are called by priority and in registration order for equal priority`() =
        runTest {
            val bus = EventBus()
            val calls = mutableListOf<String>()
            bus.subscribe(EventBus.PRIORITY_LOW) { calls += "low" }
            bus.subscribe { calls += "normal1" }
            bus.subscribe(EventBus.PRIORITY_HIGH) { calls += "high" }
            bus.subscribe { calls += "normal2" }

            bus.emit(TestEvent("a"))

            assertEquals(listOf("high", "normal1", "normal2", "low"), calls)
        }

    @Test
    fun `Cancelled handlers no longer receive events`() =
        runTest {
            val bus = EventBus()
            val received = mutableListOf<Event>()
            val registration = bus.subscribe { received += it }

            bus.emit(TestEvent("a"))
            registration.cancel()
            bus.emit(TestEvent("b"))

            assertEquals(listOf<Event>(TestEvent("a")), received)
        }

    @Test
    fun `Cancelling a later handler during emission skips it`() =
        runTest {
            val bus = EventBus()
            val received = mutableListOf<String>()
            lateinit var later: EventBus.Registration
            bus.subscribe(EventBus.PRIORITY_HIGH) { later.cancel() }
            later = bus.subscribe { received += it.action }

            bus.emit(TestEvent("a"))

            assertEquals(emptyList(), received)
        }

    @Test
    fun `Exceptions of non-throwing handlers are isolated`() =
        runTest {
            val bus = EventBus()
            val received = mutableListOf<String>()
            bus.subscribe(EventBus.PRIORITY_HIGH) { error("boom") }
            bus.subscribe { received += it.action }

            bus.emit(TestEvent("a"))

            assertEquals(listOf("a"), received)
        }

    @Test
    fun `Exceptions of throwing handlers reach the emitter and halt delivery`() =
        runTest {
            val bus = EventBus()
            val received = mutableListOf<String>()
            bus.subscribe(EventBus.PRIORITY_HIGH, throws = true) { error("boom") }
            bus.subscribe { received += it.action }

            assertFailsWith<IllegalStateException> { bus.emit(TestEvent("a")) }
            assertEquals(emptyList(), received)
        }

    @Test
    fun `Exceptions of filters are handled like handler exceptions`() =
        runTest {
            val bus = EventBus()
            val received = mutableListOf<String>()
            bus.subscribe(EventBus.PRIORITY_HIGH, filter = { error("boom") }) { received += "faulty" }
            bus.subscribe { received += it.action }

            bus.emit(TestEvent("a"))
            assertEquals(listOf("a"), received)

            bus.subscribe(EventBus.PRIORITY_HIGH, throws = true, filter = { error("boom") }) {}
            assertFailsWith<IllegalStateException> { bus.emit(TestEvent("b")) }
        }

    @Test
    fun `on delivers only events of the requested type that match the filter`() =
        runTest {
            val bus = EventBus()
            val received = mutableListOf<TestEvent>()
            bus.on<TestEvent>(filter = { it.action != "skip" }) { received += it }

            bus.emit(TestEvent("a"))
            bus.emit(OtherEvent("b"))
            bus.emit(TestEvent("skip"))

            assertEquals(listOf(TestEvent("a")), received)
        }

    @Test
    fun `once delivers a single matching event and deregisters`() =
        runTest {
            val bus = EventBus()
            val received = mutableListOf<TestEvent>()
            var filterCalls = 0
            bus.once<TestEvent>(filter = {
                filterCalls++
                it.action == "match"
            }) { received += it }

            bus.emit(TestEvent("other"))
            bus.emit(TestEvent("match"))
            bus.emit(TestEvent("match"))

            assertEquals(listOf(TestEvent("match")), received)
            // The filter must not be consulted any more once the handler deregistered itself.
            assertEquals(2, filterCalls)
        }

    @Test
    fun `once deregisters even if the handler throws`() =
        runTest {
            val bus = EventBus()
            var filterCalls = 0
            bus.once<TestEvent>(filter = {
                filterCalls++
                true
            }) { error("boom") }

            bus.emit(TestEvent("a"))
            bus.emit(TestEvent("b"))

            assertEquals(1, filterCalls)
        }

    @Test
    fun `Cancelling once before an event prevents delivery`() =
        runTest {
            val bus = EventBus()
            val received = mutableListOf<TestEvent>()
            bus.once<TestEvent> { received += it }.cancel()

            bus.emit(TestEvent("a"))

            assertEquals(emptyList(), received)
        }

    private fun stream(
        bus: EventBus,
        queueSize: Int,
    ) = EventStream(bus, ConfigManager(MapApplicationConfig("events.queueSize" to "$queueSize")))

    private fun at(
        action: String,
        millis: Long,
    ) = TestEvent(action, Instant.fromEpochMilliseconds(millis))

    @Test
    fun `EventStream delivers only new events by default`() =
        runTest {
            val bus = EventBus()
            val stream = stream(bus, 4)
            bus.emit(TestEvent("old"))

            val received = mutableListOf<Event>()
            backgroundScope.launch { stream.events().toList(received) }
            // Let the collector subscribe before emitting, then let it process the new event.
            testScheduler.runCurrent()
            bus.emit(TestEvent("new"))
            testScheduler.runCurrent()

            assertEquals(listOf<Event>(TestEvent("new")), received)
        }

    @Test
    fun `EventStream delivers retained events since the given instant followed by new events`() =
        runTest {
            val bus = EventBus()
            val stream = stream(bus, 4)
            bus.emit(at("a", 1))
            bus.emit(at("b", 2))
            bus.emit(at("c", 3))

            val received = mutableListOf<Event>()
            backgroundScope.launch { stream.events(since = Instant.fromEpochMilliseconds(2)).toList(received) }
            testScheduler.runCurrent()
            // New events are delivered regardless of their timestamp.
            bus.emit(at("d", 0))
            testScheduler.runCurrent()

            assertEquals(listOf<Event>(at("b", 2), at("c", 3), at("d", 0)), received)
        }

    @Test
    fun `EventStream retains only the configured number of events`() =
        runTest {
            val bus = EventBus()
            val stream = stream(bus, 2)
            bus.emit(TestEvent("a"))
            bus.emit(TestEvent("b"))
            bus.emit(TestEvent("c"))

            assertEquals(
                listOf<Event>(TestEvent("b"), TestEvent("c")),
                stream.events(since = Instant.fromEpochMilliseconds(0)).take(2).toList(),
            )
        }

    @Test
    fun `EventStream rejects non-positive queue sizes`() {
        assertFailsWith<IllegalArgumentException> { stream(EventBus(), 0) }
        assertFailsWith<IllegalArgumentException> { stream(EventBus(), -1) }
    }
}
