package de.fraunhofer.aisec.glue

import io.ktor.client.plugins.*
import io.ktor.client.statement.*
import io.ktor.http.*

object Utils {
    private const val HEX_DIGITS = "0123456789abcdef"

    fun ByteArray.hexEncode(): String {
        val builder = StringBuilder(size * 2)
        for (byte in this) {
            val int = byte.toInt() and 0xff
            builder.append(HEX_DIGITS[int shr 4])
            builder.append(HEX_DIGITS[int and 0x0f])
        }
        return builder.toString()
    }

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
