package de.fraunhofer.aisec.glue.proxy

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.util.*

/**
 * The proxy object is used to forward ktor server requests using a ktor HTTP client.
 * Note that this object may become a non-singleton class at some point.
 */
class Proxy {
    private val httpClient = HttpClient()

    // We need to filter out some headers from the incoming request.
    // Some (e.g. Connection) for correctness, others (e.g. Authorization) to not leak information to the
    // container.
    private val filterRequestHeaders =
        arrayOf(
            HttpHeaders.Connection,
            HttpHeaders.Authorization,
            // TODO: Is there a scenario where we would want websockets to work?
            HttpHeaders.Upgrade,
            // TODO: More headers...
        )

    suspend fun delegate(
        request: ApplicationRequest,
        proxyUrl: String,
    ): OutgoingContent {
        val resp =
            httpClient.request(proxyUrl) {
                method = request.httpMethod
                headers {
                    appendAll(
                        request.headers.filter { k, _ ->
                            !filterRequestHeaders.any { h -> k.equals(h, ignoreCase = true) }
                        },
                    )
                    // May be useful for logging, if the container webserver supports these headers
                    append("X-Forwarded-For", request.origin.remoteAddress)
                    append("Forwarded", "for=${request.origin.remoteAddress}")
                }
                setBody(request.receiveChannel())
            }
        // TODO: Filter response headers and rewrite Location header in response
        return ProxyResponse(resp)
    }
}
