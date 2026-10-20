/*
 * Copyright 2024-2026 Fraunhofer AISEC and Fraunhofer IOSB
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
package de.fraunhofer.iosb.odrl.glue

import de.fraunhofer.iosb.odrl.OdrlIdentifiers

object GlueIdentifiers {
    const val CONTEXT = OdrlIdentifiers.CONTEXT

    // Core vocabulary
    const val CONTAINER_PIPELINE = "http://fhg.de/glue/ContainerPipeline"
    const val PLATFORM_SECURITY = "http://fhg.de/glue/platformSecurity"
    const val ATLS_PROXY_IN = "http://fhg.de/glue/atlsProxyIn"
    const val ATLS_PROXY_OUT = "http://fhg.de/glue/atlsProxyOut"

    const val ATLS_PROXY_CONFIGURATION = "http://fhg.de/glue/AtlsProxyConfiguration"
    const val PROXY_MODE = "http://fhg.de/glue/proxyMode"
    const val PROXY_ENDPOINT = "http://fhg.de/glue/proxyEndpoint"

    const val ATLS_PROXY_ENDPOINT = "http://fhg.de/glue/AtlsProxyEndpoint"
    const val LISTEN_ENDPOINT = "http://fhg.de/glue/listenEndpoint"
    const val NEXT_ENDPOINT = "http://fhg.de/glue/nextEndpoint"

    const val CONTAINER = "http://fhg.de/glue/Container"
    const val HAS_CONTAINER = "http://fhg.de/glue/container"
    const val IMAGE = "http://fhg.de/glue/image"
    const val HASH = "http://fhg.de/glue/hash"
    const val ALLOW_NETWORK = "http://fhg.de/glue/allowNetwork"

    const val PARAMETER = "http://fhg.de/glue/Parameter"
    const val HAS_PARAMETER = "http://fhg.de/glue/parameter"

    const val REST_PARAMETER = "http://fhg.de/glue/RestParameter"
    const val HTTP_URL = "http://fhg.de/glue/httpUrl"
    const val HTTP_TYPE = "http://fhg.de/glue/httpType"

    const val VALUE = "http://fhg.de/glue/Value"
    const val HAS_VALUE = "http://fhg.de/glue/value"
    const val JSON = "http://fhg.de/glue/json"

    const val KEY_VALUE_PARAMETER = "http://fhg.de/glue/KeyValueParameter"
    const val PARAMETER_VALUE = "http://fhg.de/glue/parameterValue"
    const val PARAMETER_KEY = "http://fhg.de/glue/parameterKey"

    const val PARAMETER_PLACEHOLDER = "http://fhg.de/glue/ParameterPlaceholder"
    const val KEY = "http://fhg.de/glue/key"

    // Common vocabulary: Actions
    const val DEPLOY_PIPELINE = "http://fhg.de/glue/deployPipeline"

    // Common vocabulary: Left operands
    const val HAS_POLICY_TEMPLATE = "http://fhg.de/glue/policyTemplate"
    const val HAS_CONTAINER_PIPELINE = "http://fhg.de/glue/containerPipeline"
}
