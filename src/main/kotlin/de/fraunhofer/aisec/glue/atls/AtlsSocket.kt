package de.fraunhofer.aisec.glue.atls

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineDispatcher
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import javax.net.ssl.ExtendedSSLSession
import javax.net.ssl.HandshakeCompletedListener
import javax.net.ssl.SSLSession
import javax.net.ssl.SSLSocket

class AtlsSocket(
    private val inner: SSLSocket,
    private val objectMapper: ObjectMapper,
    private val cmcClient: CMCClient,
    private val dispatcher: CoroutineDispatcher,
) : SSLSocket() {
    @Volatile
    private var handshakeCompleted: Boolean = false

    override fun startHandshake() {
        LOG.atDebug {
            message = "Starting TLS handshake"
            payload = mapOf(
                "socket" to inner.remoteSocketAddress.toString()
            )
        }

        inner.startHandshake()

        if (!handshakeCompleted) {
            LOG.atDebug {
                message = "TLS handshake completed, initiating ATLS post-handshake"
                payload = mapOf(
                    "socket" to inner.remoteSocketAddress.toString()
                )
            }

            val session = inner.session as ExtendedSSLSession

            LOG.atDebug {
                message = "Extracting SSL session information"
                payload = mapOf(
                    "socket" to inner.remoteSocketAddress.toString(),
                    "protocol" to session.protocol,
                    "cipherSuite" to session.cipherSuite
                )
            }

            val channelBinding = session.exportKeyingMaterialData("EXPORTER-Channel-Binding", null, 32)

            LOG.atDebug {
                message = "Extracted channel binding data"
                payload = mapOf(
                    "socket" to inner.remoteSocketAddress.toString(),
                    "bindingLength" to channelBinding.size
                )
            }

            val cacheKey =
                session.peerCertificates.first()?.let {
                    MessageDigest.getInstance("SHA-256").digest(it.encoded).toHexString()
                }

            LOG.atDebug {
                message = "Calculated peer certificate cache key"
                payload = mapOf(
                    "socket" to inner.remoteSocketAddress.toString(),
                    "cacheKey" to (cacheKey ?: "null"),
                    "hasPeerCert" to (session.peerCertificates.firstOrNull() != null)
                )
            }

            val atlsHandshake =
                AtlsHandshake(
                    objectMapper,
                    channelBinding,
                    cmcClient,
                    AtlsHandshake.AttestationType.SERVER,
                    true,
                    cacheKey,
                    dispatcher,
                )

            LOG.atDebug {
                message = "Created ATLS handshake instance"
                payload = mapOf(
                    "socket" to inner.remoteSocketAddress.toString(),
                    "attestationType" to "SERVER",
                    "useCaching" to true
                )
            }

            val input = DataInputStream(inner.getInputStream())
            val output = DataOutputStream(inner.getOutputStream())

            val initialPacket = atlsHandshake.start()

            LOG.atDebug {
                message = "Starting ATLS handshake, sending initial packet"
                payload = mapOf(
                    "socket" to inner.remoteSocketAddress.toString(),
                    "packetSize" to initialPacket.size
                )
            }

            output.writeInt(initialPacket.size)
            output.write(initialPacket)
            output.flush()

            while (atlsHandshake.isRunning) {
                val readLength = input.readInt()
                LOG.atDebug {
                    message = "Reading ATLS packet"
                    payload = mapOf(
                        "socket" to inner.remoteSocketAddress.toString(),
                        "expectedLength" to readLength
                    )
                }

                val readPacket = input.readNBytes(readLength)
                LOG.atDebug {
                    message = "Received ATLS packet, feeding to handshake"
                    payload = mapOf(
                        "socket" to inner.remoteSocketAddress.toString(),
                        "packetSize" to readPacket.size
                    )
                }

                val writePacket = atlsHandshake.feedPacket(readPacket)

                if (writePacket != null) {
                    LOG.atDebug {
                        message = "ATLS handshake produced response packet, sending"
                        payload = mapOf(
                            "socket" to inner.remoteSocketAddress.toString(),
                            "responseSize" to writePacket.size
                        )
                    }
                    output.writeInt(writePacket.size)
                    output.write(writePacket)
                    output.flush()
                } else {
                    LOG.atDebug {
                        message = "ATLS handshake did not produce response packet"
                        payload = mapOf(
                            "socket" to inner.remoteSocketAddress.toString()
                        )
                    }
                }
            }

            if (atlsHandshake.failure != null) {
                LOG.atWarn {
                    message = "ATLS handshake failed"
                    cause = atlsHandshake.failure
                    payload = mapOf(
                        "socket" to inner.remoteSocketAddress.toString()
                    )
                }
                throw IllegalStateException("ATLS handshake failed", atlsHandshake.failure)
            }

            LOG.atDebug {
                message = "ATLS handshake completed successfully"
                payload = mapOf(
                    "socket" to inner.remoteSocketAddress.toString()
                )
            }

            handshakeCompleted = true
        }
    }

    override fun getInputStream(): InputStream? {
        if (!handshakeCompleted) {
            startHandshake()
        }
        return inner.getInputStream()
    }

    override fun getOutputStream(): OutputStream? {
        if (!handshakeCompleted) {
            startHandshake()
        }
        return inner.getOutputStream()
    }

    // All other functions are delegated to the inner socket

    override fun getSupportedCipherSuites(): Array<out String?> {
        return inner.supportedCipherSuites
    }

    override fun getEnabledCipherSuites(): Array<out String?> {
        return inner.enabledCipherSuites
    }

    override fun setEnabledCipherSuites(suites: Array<out String?>?) {
        inner.enabledCipherSuites = suites
    }

    override fun getSupportedProtocols(): Array<out String?> {
        return inner.supportedProtocols
    }

    override fun getEnabledProtocols(): Array<out String?> {
        return inner.enabledProtocols
    }

    override fun setEnabledProtocols(protocols: Array<out String?>?) {
        inner.enabledProtocols = protocols
    }

    override fun getSession(): SSLSession {
        return inner.session
    }

    override fun addHandshakeCompletedListener(listener: HandshakeCompletedListener?) {
        inner.addHandshakeCompletedListener(listener)
    }

    override fun removeHandshakeCompletedListener(listener: HandshakeCompletedListener?) {
        inner.removeHandshakeCompletedListener(listener)
    }

    override fun setUseClientMode(mode: Boolean) {
        inner.useClientMode = mode
    }

    override fun getUseClientMode(): Boolean {
        return inner.useClientMode
    }

    override fun setNeedClientAuth(need: Boolean) {
        inner.needClientAuth = need
    }

    override fun getNeedClientAuth(): Boolean {
        return inner.needClientAuth
    }

    override fun setWantClientAuth(want: Boolean) {
        inner.wantClientAuth = want
    }

    override fun getWantClientAuth(): Boolean {
        return inner.wantClientAuth
    }

    override fun setEnableSessionCreation(flag: Boolean) {
        inner.enableSessionCreation = flag
    }

    override fun getEnableSessionCreation(): Boolean {
        return inner.enableSessionCreation
    }

    // Socket method overrides with default implementations

    override fun connect(endpoint: java.net.SocketAddress?) {
        inner.connect(endpoint)
    }

    override fun connect(
        endpoint: java.net.SocketAddress?,
        timeout: Int,
    ) {
        inner.connect(endpoint, timeout)
    }

    override fun bind(bindpoint: java.net.SocketAddress?) {
        inner.bind(bindpoint)
    }

    override fun getInetAddress(): java.net.InetAddress? {
        return inner.inetAddress
    }

    override fun getLocalAddress(): java.net.InetAddress? {
        return inner.localAddress
    }

    override fun getPort(): Int {
        return inner.port
    }

    override fun getLocalPort(): Int {
        return inner.localPort
    }

    override fun getRemoteSocketAddress(): java.net.SocketAddress? {
        return inner.remoteSocketAddress
    }

    override fun getLocalSocketAddress(): java.net.SocketAddress? {
        return inner.localSocketAddress
    }

    override fun getChannel(): java.nio.channels.SocketChannel? {
        return inner.channel
    }

    override fun close() {
        inner.close()
    }

    override fun shutdownInput() {
        inner.shutdownInput()
    }

    override fun shutdownOutput() {
        inner.shutdownOutput()
    }

    override fun isConnected(): Boolean {
        return inner.isConnected
    }

    override fun isBound(): Boolean {
        return inner.isBound
    }

    override fun isClosed(): Boolean {
        return inner.isClosed
    }

    override fun isInputShutdown(): Boolean {
        return inner.isInputShutdown
    }

    override fun isOutputShutdown(): Boolean {
        return inner.isOutputShutdown
    }

    override fun setTcpNoDelay(on: Boolean) {
        inner.tcpNoDelay = on
    }

    override fun getTcpNoDelay(): Boolean {
        return inner.tcpNoDelay
    }

    override fun setSoLinger(
        on: Boolean,
        linger: Int,
    ) {
        inner.setSoLinger(on, linger)
    }

    override fun getSoLinger(): Int {
        return inner.soLinger
    }

    override fun sendUrgentData(data: Int) {
        inner.sendUrgentData(data)
    }

    override fun setOOBInline(on: Boolean) {
        inner.oobInline = on
    }

    override fun getOOBInline(): Boolean {
        return inner.oobInline
    }

    override fun setSoTimeout(timeout: Int) {
        inner.soTimeout = timeout
    }

    override fun getSoTimeout(): Int {
        return inner.soTimeout
    }

    override fun setSendBufferSize(size: Int) {
        inner.sendBufferSize = size
    }

    override fun getSendBufferSize(): Int {
        return inner.sendBufferSize
    }

    override fun setReceiveBufferSize(size: Int) {
        inner.receiveBufferSize = size
    }

    override fun getReceiveBufferSize(): Int {
        return inner.receiveBufferSize
    }

    override fun setKeepAlive(on: Boolean) {
        inner.keepAlive = on
    }

    override fun getKeepAlive(): Boolean {
        return inner.keepAlive
    }

    override fun setTrafficClass(tc: Int) {
        inner.trafficClass = tc
    }

    override fun getTrafficClass(): Int {
        return inner.trafficClass
    }

    override fun setReuseAddress(on: Boolean) {
        inner.reuseAddress = on
    }

    override fun getReuseAddress(): Boolean {
        return inner.reuseAddress
    }

    override fun setPerformancePreferences(
        connectionTime: Int,
        latency: Int,
        bandwidth: Int,
    ) {
        inner.setPerformancePreferences(connectionTime, latency, bandwidth)
    }

    // Additional SSLSocket methods

    override fun getHandshakeSession(): SSLSession? {
        return inner.handshakeSession
    }

    override fun getSSLParameters(): javax.net.ssl.SSLParameters {
        return inner.sslParameters
    }

    override fun setSSLParameters(params: javax.net.ssl.SSLParameters?) {
        inner.sslParameters = params
    }

    override fun getApplicationProtocol(): String? {
        return inner.applicationProtocol
    }

    override fun getHandshakeApplicationProtocol(): String? {
        return inner.handshakeApplicationProtocol
    }

    override fun setHandshakeApplicationProtocolSelector(selector: java.util.function.BiFunction<SSLSocket, MutableList<String>, String>?) {
        inner.handshakeApplicationProtocolSelector = selector
    }

    override fun getHandshakeApplicationProtocolSelector(): java.util.function.BiFunction<SSLSocket, MutableList<String>, String>? {
        return inner.handshakeApplicationProtocolSelector
    }

    companion object {
        private val LOG = KotlinLogging.logger {}
    }
}
