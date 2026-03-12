package de.fraunhofer.aisec.glue.pipeline.atls

import de.fraunhofer.aisec.glue.config.Config
import io.ktor.network.sockets.*
import io.ktor.server.config.*
import java.net.URI

class AtlsConfig(rawConfig: ApplicationConfig) : Config(rawConfig) {
    val enabled by withDefault(boolProp(), true)

    // TODO: This value MUST NOT be configurable externally in a production environment!
    val proxyContainer by withDefault(
        stringProp(),
        "container-registry.gitlab.cc-asp.fraunhofer.de/ccit-glue/usage-control/container-pipeline/atls-proxy:running-example",
    )

    // TODO: This value MUST NOT be configurable externally in a production environment!
    val proxyHash by withDefault(
        stringProp(),
        "sha256:ea0ffc24a3dc58455489f1c885244709d518aedfcbec965229b0f9191aa578ef",
    )

    // TODO: Move to somewhere appropriate - values are appropriate for the connector VMs
    val cmcAddress by withDefault(
        convert(stringProp()) {
            val uri = URI(it)
            when (uri.scheme) {
                "unix" -> UnixSocketAddress(uri.path)
                "tcp" -> InetSocketAddress(uri.host, uri.port)
                else -> throw IllegalArgumentException("Invalid scheme for the CMC address: ${uri.scheme}")
            }
        },
        UnixSocketAddress("/run/cmcd.sock"),
    )

    val caCertPath by withDefault(stringProp(), "/var/ca.pem")
}
