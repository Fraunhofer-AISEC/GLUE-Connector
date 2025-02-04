package de.fraunhofer.aisec.glue

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
        val username = System.getenv("AUTH_USERNAME") ?: throw IllegalStateException("Auth username not set")
        val password = System.getenv("AUTH_PASSWORD") ?: throw IllegalStateException("Auth password not set")
        return Pair(username, password)
    }
}
