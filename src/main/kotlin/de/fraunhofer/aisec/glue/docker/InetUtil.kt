/*
 * Copyright 2024 Fraunhofer AISEC
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
package de.fraunhofer.aisec.glue.docker

import java.net.Inet4Address
import java.nio.ByteBuffer

object InetUtil {
    fun parseInet4Address(address: String): Inet4Address {
        val bytes = address.split('.')
        if (bytes.size != 4) {
            throw IllegalArgumentException("Parsed IPv4 address does not contain four bytes")
        }
        return Inet4Address.getByAddress(bytes.map(String::toByte).toByteArray()) as Inet4Address
    }

    fun getSubnets(
        baseAddress: Inet4Address,
        prefixLen: Int,
        subnetLen: Int,
    ): Sequence<Inet4Address> {
        val base = ByteBuffer.wrap(baseAddress.address).getInt()
        val subnets = 1 shl (subnetLen - prefixLen)

        return (0..<subnets)
            .map { i ->
                val subnetAddr = base or (i shl (32 - subnetLen))
                val subnetAddrBuffer = ByteBuffer.allocate(4).putInt(subnetAddr).array()
                Inet4Address.getByAddress(subnetAddrBuffer) as Inet4Address
            }.asSequence()
    }
}
