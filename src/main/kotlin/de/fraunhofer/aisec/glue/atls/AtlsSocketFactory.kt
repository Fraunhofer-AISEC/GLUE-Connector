package de.fraunhofer.aisec.glue.atls

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.CoroutineDispatcher
import java.net.InetAddress
import java.net.Socket
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

class AtlsSocketFactory(
    private val objectMapper: ObjectMapper,
    private val cmcClient: CMCClient,
    private val dispatcher: CoroutineDispatcher,
    inner: SSLSocketFactory?,
) : SSLSocketFactory() {
    private val inner: SSLSocketFactory = inner ?: (getDefault() as SSLSocketFactory)

    override fun getDefaultCipherSuites(): Array<out String?>? = inner.defaultCipherSuites

    override fun getSupportedCipherSuites(): Array<out String?>? = inner.supportedCipherSuites

    override fun createSocket(
        s: Socket?,
        host: String?,
        port: Int,
        autoClose: Boolean,
    ): Socket = AtlsSocket(inner.createSocket(s, host, port, autoClose) as SSLSocket, objectMapper, cmcClient, dispatcher)

    override fun createSocket(
        host: String?,
        port: Int,
    ): Socket = AtlsSocket(inner.createSocket(host, port) as SSLSocket, objectMapper, cmcClient, dispatcher)

    override fun createSocket(
        host: String?,
        port: Int,
        localHost: InetAddress?,
        localPort: Int,
    ): Socket =
        AtlsSocket(
            inner.createSocket(host, port, localHost, localPort) as SSLSocket,
            objectMapper,
            cmcClient,
            dispatcher,
        )

    override fun createSocket(
        host: InetAddress?,
        port: Int,
    ): Socket = AtlsSocket(inner.createSocket(host, port) as SSLSocket, objectMapper, cmcClient, dispatcher)

    override fun createSocket(
        address: InetAddress?,
        port: Int,
        localAddress: InetAddress?,
        localPort: Int,
    ): Socket =
        AtlsSocket(
            inner.createSocket(address, port, localAddress, localPort) as SSLSocket,
            objectMapper,
            cmcClient,
            dispatcher,
        )
}
