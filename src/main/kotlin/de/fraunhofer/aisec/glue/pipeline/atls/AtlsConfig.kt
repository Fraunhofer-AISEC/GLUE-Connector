package de.fraunhofer.aisec.glue.pipeline.atls

import de.fraunhofer.aisec.glue.config.Config
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.UnixSocketAddress
import io.ktor.server.config.ApplicationConfig
import java.net.URI

class AtlsConfig(rawConfig: ApplicationConfig) : Config(rawConfig) {
    val proxyContainer by withDefault(
        stringProp(),
        "container-registry.gitlab.cc-asp.fraunhofer.de/ccit-glue/usage-control/container-pipeline/atls-proxy:feature-cmc-integration",
    )

    // TODO: Move to somewhere appropriate - values are appropriate for the connector VMs
    val cmcAddress by withDefault(
        convert(stringProp(), {
            val uri = URI(it)
            when (uri.scheme) {
                "unix" -> UnixSocketAddress(uri.path)
                "tcp" -> InetSocketAddress(uri.host, uri.port)
                else -> throw IllegalArgumentException("Invalid scheme for the CMC address: ${uri.scheme}")
            }
        }),
        UnixSocketAddress("/run/cmcd.sock"),
    )

    val caCertPath by withDefault(stringProp(), "/var/ca.pem")
}
