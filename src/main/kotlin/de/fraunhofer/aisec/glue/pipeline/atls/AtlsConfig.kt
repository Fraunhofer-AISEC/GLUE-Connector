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
        "sha256:26e52ffc123abefe919289feaaa046f2afd702d62d82006d17b9d2c1e1431782",
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
