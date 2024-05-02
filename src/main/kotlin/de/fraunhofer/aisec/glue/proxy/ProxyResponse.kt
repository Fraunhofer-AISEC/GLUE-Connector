package de.fraunhofer.aisec.glue.proxy

import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.util.*
import io.ktor.utils.io.*

/**
 * A ktor server response that wraps a ktor client response
 */
class ProxyResponse(private val proxiedResponse: HttpResponse) : OutgoingContent.WriteChannelContent() {
    override val contentLength: Long? = proxiedResponse.contentLength()
    override val contentType: ContentType? = proxiedResponse.contentType()
    override val status: HttpStatusCode = proxiedResponse.status

    override val headers: Headers =
        Headers.build {
            appendAll(
                proxiedResponse.headers.filter { k, _ ->
                    k.equals(HttpHeaders.ContentType, ignoreCase = true) &&
                        k.equals(HttpHeaders.ContentLength, ignoreCase = true)
                },
            )
        }

    override suspend fun writeTo(channel: ByteWriteChannel) {
        proxiedResponse.bodyAsChannel().copyTo(channel)
    }
}
