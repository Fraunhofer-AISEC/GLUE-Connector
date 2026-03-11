package de.fraunhofer.aisec.glue.tools

import java.util.*
import kotlin.math.ceil

class LRUCache<K, V> private constructor(
    private val delegate: MutableMap<K, V>,
) : MutableMap<K, V> by delegate {
    constructor(capacity: Int, loadFactor: Float = 0.75f) :
        this(createSynchronizedDelegate(capacity, loadFactor)) {
        require(capacity > 0) { "capacity must be > 0" }
    }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() =
            synchronized(delegate) {
                delegate.entries
                    .map { SimpleEntry(it.key, it.value) }
                    .toMutableSet()
            }

    override val keys: MutableSet<K>
        get() = synchronized(delegate) { delegate.keys.toMutableSet() }

    override val values: MutableCollection<V>
        get() = synchronized(delegate) { delegate.values.toMutableList() }

    private data class SimpleEntry<K, V>(override val key: K, override var value: V) : MutableMap.MutableEntry<K, V> {
        override fun setValue(newValue: V): V {
            val previous = value
            value = newValue
            return previous
        }
    }

    companion object {
        fun <K, V> createSynchronizedDelegate(
            capacity: Int,
            loadFactor: Float,
        ): MutableMap<K, V> =
            Collections.synchronizedMap(
                object : LinkedHashMap<K, V>(
                    ceil((capacity + 1).toFloat() / loadFactor).toInt(),
                    loadFactor,
                    true,
                ) {
                    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?) = size > capacity
                },
            )
    }
}
