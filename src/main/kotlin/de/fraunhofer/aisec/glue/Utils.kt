/*
 * Copyright 2024-2026 Fraunhofer AISEC
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
package de.fraunhofer.aisec.glue

import io.ktor.client.plugins.*
import io.ktor.client.statement.*
import io.ktor.http.*

object Utils {
    /**
     * Get the base url that a request has been made against.
     * This is useful when we want to find out the exact point our API is reachable from, provided all reverse proxies
     * in front of us set the correct forwarding headers.
     */
    @Deprecated("Origin can easily be spoofed, using connectorConfig.publicUrl instead")
    fun baseUrlForOrigin(
        origin: RequestConnectionPoint,
        backtrack: Int,
    ): Url {
        val segments = origin.uri.split('/').dropLast(backtrack)
        val protocol = URLProtocol.createOrDefault(origin.scheme)
        val builder =
            URLBuilder(
                protocol = protocol,
                host = origin.serverHost,
                pathSegments = segments,
            )
        if (protocol.defaultPort != origin.serverPort) {
            builder.port = origin.serverPort
        }
        return builder.build()
    }

    fun getCredentials(): Pair<String, String> {
        val username = System.getenv("AUTH_USERNAME") ?: "ids"
        val password = System.getenv("AUTH_PASSWORD") ?: "ids"
        return Pair(username, password)
    }

    suspend fun makeHttpException(
        resp: HttpResponse,
        body: String? = null,
    ): ResponseException {
        assert(!resp.status.isSuccess())
        val ctor =
            when (resp.status.value) {
                in 300..399 -> ::RedirectResponseException
                in 400..499 -> ::ClientRequestException
                in 500..599 -> ::ServerResponseException
                else -> ::ResponseException
            }
        return ctor(resp, body ?: resp.bodyAsText())
    }

    /**
     * Return the string truncated to a maximum length followed by an ellipsis to indicate more data.
     * Note that the maximum length applies to the full length of the output string, including the ellipsis characters.
     */
    fun String.truncate(
        maxLength: Int,
        ellipsis: String = "...",
    ): String {
        if (length <= maxLength) {
            return this
        }
        return "${take(maxLength - 3)}$ellipsis"
    }
}
