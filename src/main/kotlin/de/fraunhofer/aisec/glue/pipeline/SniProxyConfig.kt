/*
 * Copyright 2026 Fraunhofer AISEC
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

import de.fraunhofer.aisec.glue.config.BasePath
import de.fraunhofer.aisec.glue.config.Config
import io.ktor.server.config.*

/**
 * Configuration for exposing pipelines through an SNI proxy.
 *
 * By default, a pipeline is exposed by publishing its entry port on a port chosen by the backend.
 * Alternatively, pipelines can be exposed on a single, shared port through a TLS proxy that routes connections based on
 * the TLS server name indication (SNI), for example Envoy, HAProxy or Traefik.
 * In that case, every pipeline gets its own DNS name `<pipelineId>.<pipelineHost>`
 * (see [de.fraunhofer.aisec.glue.config.ConnectorConfig.pipelineHost]) that the proxy routes to its entry container.
 */
@BasePath("sniProxy")
class SniProxyConfig(
    rawConfig: ApplicationConfig,
) : Config(rawConfig) {
    /**
     * Expose pipelines through an SNI proxy instead of publishing their ports.
     */
    val enabled by withDefault(boolProp(), false)

    /**
     * The public port the SNI proxy listens on.
     * When unset, the port of [de.fraunhofer.aisec.glue.config.ConnectorConfig.publicUrl] is used, as the proxy
     * typically serves both the connector and its pipelines.
     */
    val port by intProp()
}
