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
package de.fraunhofer.aisec.glue.plugins

import de.fraunhofer.aisec.glue.Constants
import de.fraunhofer.aisec.glue.tools.LRUCache
import io.ktor.client.*
import io.ktor.client.plugins.api.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.util.*

class ClientEdcCompatTransformationConfig {
    var transformRequest: (Any) -> Any = { it }
    var cacheCapacity: Int = 1024
}

val EdcCompatPeerEndpointAttribute = AttributeKey<String>("edcCompatPeerEndpoint")
private val EdcCompatSkipProbeAttribute = AttributeKey<Boolean>("edcCompatSkipProbe")

val ClientEdcCompatTransformation =
    createClientPlugin(name = "ClientBodyTransformation", ::ClientEdcCompatTransformationConfig) {
        val config = pluginConfig
        val trustedConnectorServerCache = LRUCache<String, Boolean>(config.cacheCapacity)

        on(TransformRequestBodyObject) { requestBuilder, body ->
            if (requestBuilder.shouldSkipEdcCompatTransformation(client, trustedConnectorServerCache)) {
                return@on body
            }
            config.transformRequest(body)
        }
    }

private object TransformRequestBodyObject : ClientHook<suspend (HttpRequestBuilder, Any) -> Any> {
    override fun install(
        client: HttpClient,
        handler: suspend (HttpRequestBuilder, Any) -> Any,
    ) {
        client.requestPipeline.intercept(HttpRequestPipeline.Transform) {
            if (subject is OutgoingContent) {
                return@intercept
            }

            val transformed = handler(context, subject)
            proceedWith(transformed)
        }
    }
}

private suspend fun HttpRequestBuilder.shouldSkipEdcCompatTransformation(
    client: HttpClient,
    trustedConnectorServerCache: LRUCache<String, Boolean>,
): Boolean {
    // Prevents recursion for HEAD probe requests
    if (attributes.contains(EdcCompatSkipProbeAttribute)) {
        return true
    }

    // We use either the URL prefix we got from upstream or assume that no EDC compat
    // transformations are required.
    val cacheKey = getPeerEndpointPrefixOrFullRequestUrl() ?: return true

    trustedConnectorServerCache[cacheKey]?.let { return it }

    // If we did not find a match, we probe the server with the "cacheKey" as HEAD URL.
    val shouldSkip =
        runCatching {
            client.head(cacheKey) {
                attributes.put(EdcCompatSkipProbeAttribute, true)
            }
        }.getOrNull()
            ?.headers
            ?.getAll(HttpHeaders.Server)
            .orEmpty()
            .any(Constants.TC_USER_AGENT_REGEX::containsMatchIn)
    trustedConnectorServerCache[cacheKey] = shouldSkip
    return shouldSkip
}

private fun HttpRequestBuilder.getPeerEndpointPrefixOrFullRequestUrl(): String? = attributes.getOrNull(EdcCompatPeerEndpointAttribute)
