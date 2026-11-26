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
package de.fraunhofer.aisec.glue.config

import io.ktor.http.*
import io.ktor.server.config.*
import java.net.InetAddress

/**
 * Configuration for the connector itself.
 */
class ConnectorConfig(
    inner: ApplicationConfig,
) : Config(inner) {
    /**
     * The identity of this connector that is used as assigner and assignee during contract negotiation.
     */
    val identity by required(stringProp())

    /**
     * The public URL of this connector's inter-connector API.
     * It is used as the callback URL when starting a negotiation and its host is added to the connector's aTLS
     * certificate.
     * When this property is not set, the local IP address is used, which will likely not be routable in production.
     */
    val publicUrl by withDefault(
        convert(
            stringProp(),
            ::Url,
        ),
        URLBuilder(host = InetAddress.getLocalHost().hostAddress, port = 8080).build(),
    )

    private val pipelineHostProp by stringProp("pipelineHost")

    /**
     * The host under which external peers reach this connector's pipelines.
     * It defaults to the host component of the [publicUrl] configuration property.
     *
     * When using SNI proxy mode, the host must be a valid DNS name.
     * Pipelines are registered as subdomains by their pipeline ID under this host name.
     * How this is done depends on the pipeline backend in use.
     *
     * When exposing pipelines on ephemeral ports, this property shall contain the host name that the container is
     * reachable on.
     * Peers are expected to directly connect to these ports, potentially conflicting with firewall rules.
     */
    val pipelineHost: String get() = pipelineHostProp ?: publicUrl.host

    enum class EdcMode {
        NONE,
        COMPAT,
    }

    val edcMode by withDefault(
        convert(stringProp()) { EdcMode.valueOf(it.uppercase()) },
        EdcMode.COMPAT,
    )
}
