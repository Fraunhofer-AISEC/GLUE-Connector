/*
 * Copyright 2025-2026 Fraunhofer AISEC
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
package de.fraunhofer.aisec.glue.pipeline.atls

import de.fraunhofer.aisec.glue.atls.AtlsHandshake
import de.fraunhofer.aisec.glue.config.Config
import io.ktor.network.sockets.*
import io.ktor.server.config.*
import java.net.URI

class AtlsConfig(
    rawConfig: ApplicationConfig,
) : Config(rawConfig) {
    val enabled by withDefault(boolProp(), true)

    val proxyContainer by withDefault(
        stringProp(),
        "fraunhoferaisec/atls-proxy:latest",
    )

    val proxyHash by withDefault(
        stringProp(),
        "sha256:7c3fc337862216f1960ab29f56c04b95ba5bcdb7c050373890398e0df0a361d2",
    )

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

    /**
     * Support mutual and server attestation. Applies to connector and ATLS proxies.
     */
    val attest by withDefault(
        convert(stringProp()) {
            val type = AtlsHandshake.AttestationType.valueOf(it.uppercase())
            require(type == AtlsHandshake.AttestationType.MUTUAL || type == AtlsHandshake.AttestationType.SERVER) {
                "Unsupported ATLS attestation mode $it, must be mutual or server"
            }
            type
        },
        AtlsHandshake.AttestationType.MUTUAL,
    )
}
