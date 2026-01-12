package de.fraunhofer.aisec.glue.pipeline.atls

import de.fraunhofer.aisec.glue.config.Config
import io.ktor.network.sockets.*
import io.ktor.server.config.*
import java.net.URI

class AtlsConfig(rawConfig: ApplicationConfig) : Config(rawConfig) {
    val proxyContainer by withDefault(
        stringProp(),
        "container-registry.gitlab.cc-asp.fraunhofer.de/ccit-glue/usage-control/container-pipeline/atls-proxy:latest",
    )

    val proxyHash by withDefault(
        stringProp(),
        "sha256:bdd2cb41467018c5a2431c3c8910a06b20cf9b43b867d3d7ab0da7bad9d63669",
    )

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
