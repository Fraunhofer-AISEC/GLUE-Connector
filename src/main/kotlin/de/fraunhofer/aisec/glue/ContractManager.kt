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
package de.fraunhofer.aisec.glue

import de.fraunhofer.aisec.glue.config.Config
import de.fraunhofer.aisec.glue.config.ConfigManager
import de.fraunhofer.aisec.glue.events.EventBus
import de.fraunhofer.aisec.glue.events.on
import de.fraunhofer.aisec.glue.models.negotiation.NegotiationState
import de.fraunhofer.aisec.glue.negotiation.builtin.NegotiationUpdatedEvent
import de.fraunhofer.aisec.glue.tools.LRUCache
import de.fraunhofer.iosb.odrl.OdrlAgreement
import de.fraunhofer.iosb.odrl.OdrlPolicy
import io.ktor.server.config.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Provides access to contracts, including those of negotiations that are about to be finalized.
 *
 * A peer may use an agreement as soon as it considers the negotiation finalized, which can be slightly before this
 * connector does. Lookups for the agreement of a negotiation in the `AGREED` or `VERIFIED` state therefore wait for
 * that negotiation to complete, up to the configured `contracts.awaitTimeout`.
 */
class ContractManager(
    private val dataStore: DataStore,
    eventBus: EventBus,
    configManager: ConfigManager,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
) {
    private val config = configManager.getConfig(::ContractsConfig)
    private val cache = LRUCache<String, OdrlPolicy>(config.cacheCapacity)

    // Agreements of negotiations that are neither finalized nor terminated, indexed by the agreement ID.
    // An entry is only removed once the negotiation completed, i.e., after a finalized agreement has been stored.

    private val pending = ConcurrentHashMap<String, CompletableDeferred<OdrlAgreement?>>()

    init {
        // The data store stores agreements with a higher priority, so they are stored before any waiter is released.
        eventBus.on<NegotiationUpdatedEvent> { (data) ->
            val agreement = data.contract as? OdrlAgreement ?: return@on
            val id = agreement.uid.toString()
            when (data.state) {
                NegotiationState.AGREED, NegotiationState.VERIFIED -> {
                    pending.putIfAbsent(id, CompletableDeferred())
                }

                NegotiationState.FINALIZED -> {
                    cache[id] = agreement
                    pending.remove(id)?.complete(agreement)
                }

                NegotiationState.TERMINATED -> {
                    pending.remove(id)?.complete(null)
                }

                else -> {}
            }
        }

        // Negotiations restored from the data store do not necessarily emit an update event.
        // If one completes while this is running, a stale entry may remain. As its agreement is already stored, no
        // lookup waits for it.
        scope.launch {
            for (data in dataStore.negotiations()) {
                val agreement = data.contract as? OdrlAgreement ?: continue
                if (data.state == NegotiationState.AGREED || data.state == NegotiationState.VERIFIED) {
                    pending.putIfAbsent(agreement.uid.toString(), CompletableDeferred())
                }
            }
        }
    }

    /**
     * Get the contract with the given [id].
     * If it is not stored yet but the agreement of a pending negotiation, wait for that negotiation to complete.
     *
     * @return The contract, or `null` if it does not exist, its negotiation was terminated, or it did not complete
     * in time.
     */
    suspend fun getContract(id: String): OdrlPolicy? {
        cache[id]?.let { return it }

        // Look up the pending negotiation before querying the data store. Otherwise, the negotiation could complete
        // in between, causing both lookups to fail.
        val negotiation = pending[id]
        dataStore.getContract(id)?.let {
            cache[id] = it
            return it
        }

        return negotiation?.let { withTimeoutOrNull(config.awaitTimeout) { it.await() } }
    }

    class ContractsConfig(
        raw: ApplicationConfig,
    ) : Config(raw) {
        /**
         * How long a lookup waits for a pending negotiation, e.g., `3s` or `500ms`.
         */
        val awaitTimeout by withDefault(convert(stringProp()) { Duration.parse(it) }, 3.seconds)

        /**
         * The maximum number of contracts kept in memory.
         */
        val cacheCapacity by withDefault(intProp(), 256)
    }
}
