package de.fraunhofer.aisec.uc_container_orchestration.docker_client

import java.net.Inet4Address
import java.nio.ByteBuffer

fun parseInet4Address(address: String): Inet4Address {
    val bytes = address.split('.')
    if (bytes.size != 4) {
        throw IllegalArgumentException("Parsed IPv4 address does not contain four bytes")
    }
    return Inet4Address.getByAddress(bytes.map(String::toByte).toByteArray()) as Inet4Address
}

fun getSubnets(baseAddress: Inet4Address, prefixLen: Int, subnetLen: Int): Sequence<Inet4Address> {
    val base = ByteBuffer.wrap(baseAddress.address).getInt()
    val subnets = 1 shl (subnetLen - prefixLen)

    return (0 ..< subnets).map { i ->
        val subnetAddr = base or (i shl (32 - subnetLen))
        val subnetAddrBuffer = ByteBuffer.allocate(4).putInt(subnetAddr).array()
        Inet4Address.getByAddress(subnetAddrBuffer) as Inet4Address
    }.asSequence()
}