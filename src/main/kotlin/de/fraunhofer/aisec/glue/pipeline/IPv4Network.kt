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
package de.fraunhofer.aisec.glue.pipeline

data class IPv4Network(
    val address: IPv4Address,
    val prefixLength: Int,
) {
    init {
        require((0..32).contains(prefixLength)) { "IP prefix out of range" }
        require(address.networkAddress(prefixLength) == address) {
            "Network address $address has host bits set for prefix /$prefixLength"
        }
    }

    /** Returns the [index]-th host address in this network, e.g. `host(1)` for the first usable address. */
    fun host(index: UInt): IPv4Address {
        if (index >= size) {
            throw IllegalArgumentException("Host index $index out of range for /$prefixLength network")
        }
        return IPv4Address(address.value or index)
    }

    /** Splits this network into all subnets with the given prefix length, in ascending order. */
    fun subnets(subnetPrefixLength: Int): Sequence<IPv4Network> {
        require((prefixLength..32).contains(subnetPrefixLength)) {
            "Subnet prefix must be between $prefixLength and 32"
        }
        val count = 1uL shl (subnetPrefixLength - prefixLength)
        val step = 1uL shl (32 - subnetPrefixLength)
        return generateSequence(0uL) { it + 1uL }
            .takeWhile { it < count }
            .map { i -> IPv4Network(IPv4Address((address.value.toULong() + i * step).toUInt()), subnetPrefixLength) }
    }

    private val size: ULong
        get() = 1uL shl (32 - prefixLength)

    override fun toString(): String = "$address/$prefixLength"

    companion object {
        fun parse(encoded: String): IPv4Network {
            val parts = encoded.split('/')
            require(parts.size == 2) {
                "IPv4 network must be in CIDR notation"
            }
            return IPv4Network(IPv4Address.parse(parts[0]), parts[1].toInt())
        }
    }
}
