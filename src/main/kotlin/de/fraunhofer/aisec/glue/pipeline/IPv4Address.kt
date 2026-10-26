/*
 * Copyright 2025 Fraunhofer AISEC
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

@JvmInline
value class IPv4Address(
    val value: UInt,
) {
    override fun toString(): String =
        "${(value shr 24) and 0xffu}.${(value shr 16) and 0xffu}.${(value shr 8) and 0xffu}.${value and 0xffu}"

    fun networkAddress(prefixLength: Int): IPv4Address {
        if (!(0..32).contains(prefixLength)) {
            throw IllegalArgumentException("IP prefix out of range")
        }
        // Shifting a 32-bit value by 32 is a no-op on the JVM, so /0 needs special handling
        val mask = if (prefixLength == 0) 0u else UInt.MAX_VALUE shl (32 - prefixLength)
        return IPv4Address(value and mask)
    }

    companion object {
        fun parse(encoded: String): IPv4Address {
            val segments = encoded.split('.').map(String::toUInt)
            if (segments.size != 4) {
                throw IllegalArgumentException("IPv4 address must consist of four segments")
            }
            if (segments.any { s -> s > 255u }) {
                throw IllegalArgumentException("All IPv4 address segments must be between 0 and 255")
            }
            return IPv4Address(segments.fold(0u) { a, s -> (a shl 8) or s })
        }
    }
}
