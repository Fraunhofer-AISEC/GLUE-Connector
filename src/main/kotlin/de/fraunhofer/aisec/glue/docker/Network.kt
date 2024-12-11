package de.fraunhofer.aisec.glue.docker

import io.ktor.client.request.*
import io.ktor.http.*

class Network internal constructor(private val client: DockerClient, val name: String) {
    suspend fun connect(
        container: Container,
        address: String? = null,
    ) {
        val args =
            mapOf(
                "Container" to container.name,
                "EndpointConfig" to
                    mapOf(
                        "IPAMConfig" to
                            mapOf(
                                "IPv4Address" to address,
                            ),
                    ),
            )

        client.httpClient.post(client.endpoint("networks/$name/connect")) {
            setBody(args)
            contentType(ContentType.Application.Json)
        }
    }

    suspend fun disconnect(
        container: Container,
        force: Boolean = false,
    ) {
        val args =
            mapOf(
                "Container" to container.name,
                "Force" to force,
            )

        client.httpClient.post(client.endpoint("networks/$name/disconnect")) {
            setBody(args)
            contentType(ContentType.Application.Json)
        }
    }

    suspend fun remove() {
        client.httpClient.delete(client.endpoint("networks/$name"))
    }

    companion object {
        // We need 2 bits for the network, broadcast addresses and 2 container addresses.
        // Docker also assigns an address to the host, which means we need another bit.
        private const val LINK_NET_LEN = 29
        private val LINK_NAMES = generateSequence(0, Int::inc).map { i -> "link-$i" }.iterator()
        private val LINK_NETS = InetUtil.getSubnets(InetUtil.parseInet4Address("10.1.0.0"), 16, LINK_NET_LEN).iterator()

        suspend fun link(
            client: DockerClient,
            from: Container,
            to: Container,
        ): Network {
            val net = client.networks.create(LINK_NAMES.next(), "${LINK_NETS.next().hostAddress}/$LINK_NET_LEN")
            net.connect(from)
            net.connect(to)
            return net
        }
    }
}
