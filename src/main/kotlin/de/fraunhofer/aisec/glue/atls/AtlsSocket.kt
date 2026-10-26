/*
 * Copyright 2025-2026 Fraunhofer AISEC
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
package de.fraunhofer.aisec.glue.atls

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import java.io.DataInputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
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
            payload =
                mapOf(
                    "socket" to inner.remoteSocketAddress.toString(),
                )
        }

        inner.startHandshake()

        if (!handshakeCompleted) {
            LOG.atDebug {
                message = "TLS handshake completed, initiating ATLS post-handshake"
                payload =
                    mapOf(
                        "socket" to inner.remoteSocketAddress.toString(),
                    )
            }

            val session = inner.session as ExtendedSSLSession

            LOG.atDebug {
                message = "Extracting SSL session information"
                payload =
                    mapOf(
                        "socket" to inner.remoteSocketAddress.toString(),
                        "protocol" to session.protocol,
                        "cipherSuite" to session.cipherSuite,
                    )
            }

            val (clientBinding, serverBinding) = AtlsHandshake.createClientServerBindings(session)
            val cacheKey =
                session.peerCertificates.first()?.let {
                    MessageDigest.getInstance("SHA-256").digest(it.encoded).toHexString()
                }

            LOG.atDebug {
                message = "Calculated peer certificate cache key"
                payload =
                    mapOf(
                        "socket" to inner.remoteSocketAddress.toString(),
                        "cacheKey" to (cacheKey ?: "null"),
                        "hasPeerCert" to (session.peerCertificates.firstOrNull() != null),
                    )
            }

            val atlsHandshake =
                AtlsHandshake(
                    objectMapper,
                    clientBinding,
                    serverBinding,
                    cmcClient,
                    AtlsHandshake.AttestationType.MUTUAL,
                    true,
                    cacheKey,
                )

            LOG.atDebug {
                message = "Created ATLS handshake instance"
                payload =
                    mapOf(
                        "socket" to inner.remoteSocketAddress.toString(),
                        "attestationType" to "SERVER",
                        "useCaching" to true,
                    )
            }

            val input = DataInputStream(inner.getInputStream())
            val output = inner.getOutputStream()

            val initialPacket = runBlocking(dispatcher) { atlsHandshake.start() }

            LOG.atDebug {
                message = "Starting ATLS handshake, sending initial packet"
                payload =
                    mapOf(
                        "socket" to inner.remoteSocketAddress.toString(),
                        "packetSize" to initialPacket.size,
                    )
            }

            ByteBuffer.allocate(4 + initialPacket.size).let { buffer ->
                buffer.putInt(initialPacket.size)
                buffer.put(initialPacket)
                output.write(buffer.array())
                output.flush()
            }

            while (atlsHandshake.isRunning) {
                val readLength = input.readInt()
                LOG.atDebug {
                    message = "Reading ATLS packet"
                    payload =
                        mapOf(
                            "socket" to inner.remoteSocketAddress.toString(),
                            "expectedLength" to readLength,
                        )
                }

                val readPacket = input.readNBytes(readLength)
                LOG.atDebug {
                    message = "Received ATLS packet, feeding to handshake"
                    payload =
                        mapOf(
                            "socket" to inner.remoteSocketAddress.toString(),
                            "packetSize" to readPacket.size,
                        )
                }

                val writePacket = runBlocking(dispatcher) { atlsHandshake.feedPacket(readPacket) }

                if (writePacket != null) {
                    LOG.atDebug {
                        message = "ATLS handshake produced response packet, sending"
                        payload =
                            mapOf(
                                "socket" to inner.remoteSocketAddress.toString(),
                                "responseSize" to writePacket.size,
                            )
                    }

                    val buffer = ByteBuffer.allocate(4 + writePacket.size)
                    buffer.putInt(writePacket.size)
                    buffer.put(writePacket)
                    output.write(buffer.array())
                    output.flush()
                } else {
                    LOG.atDebug {
                        message = "ATLS handshake did not produce response packet"
                        payload =
                            mapOf(
                                "socket" to inner.remoteSocketAddress.toString(),
                            )
                    }
                }
            }

            if (atlsHandshake.failure != null) {
                LOG.atWarn {
                    message = "ATLS handshake failed"
                    cause = atlsHandshake.failure
                    payload =
                        mapOf(
                            "socket" to inner.remoteSocketAddress.toString(),
                        )
                }
                throw IllegalStateException("ATLS handshake failed", atlsHandshake.failure)
            }

            LOG.atDebug {
                message = "ATLS handshake completed successfully"
                payload =
                    mapOf(
                        "socket" to inner.remoteSocketAddress.toString(),
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

    override fun getSupportedCipherSuites(): Array<out String?> = inner.supportedCipherSuites

    override fun getEnabledCipherSuites(): Array<out String?> = inner.enabledCipherSuites

    override fun setEnabledCipherSuites(suites: Array<out String?>?) {
        inner.enabledCipherSuites = suites
    }

    override fun getSupportedProtocols(): Array<out String?> = inner.supportedProtocols

    override fun getEnabledProtocols(): Array<out String?> = inner.enabledProtocols

    override fun setEnabledProtocols(protocols: Array<out String?>?) {
        inner.enabledProtocols = protocols
    }

    override fun getSession(): SSLSession = inner.session

    override fun addHandshakeCompletedListener(listener: HandshakeCompletedListener?) {
        inner.addHandshakeCompletedListener(listener)
    }

    override fun removeHandshakeCompletedListener(listener: HandshakeCompletedListener?) {
        inner.removeHandshakeCompletedListener(listener)
    }

    override fun setUseClientMode(mode: Boolean) {
        inner.useClientMode = mode
    }

    override fun getUseClientMode(): Boolean = inner.useClientMode

    override fun setNeedClientAuth(need: Boolean) {
        inner.needClientAuth = need
    }

    override fun getNeedClientAuth(): Boolean = inner.needClientAuth

    override fun setWantClientAuth(want: Boolean) {
        inner.wantClientAuth = want
    }

    override fun getWantClientAuth(): Boolean = inner.wantClientAuth

    override fun setEnableSessionCreation(flag: Boolean) {
        inner.enableSessionCreation = flag
    }

    override fun getEnableSessionCreation(): Boolean = inner.enableSessionCreation

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

    override fun getInetAddress(): java.net.InetAddress? = inner.inetAddress

    override fun getLocalAddress(): java.net.InetAddress? = inner.localAddress

    override fun getPort(): Int = inner.port

    override fun getLocalPort(): Int = inner.localPort

    override fun getRemoteSocketAddress(): java.net.SocketAddress? = inner.remoteSocketAddress

    override fun getLocalSocketAddress(): java.net.SocketAddress? = inner.localSocketAddress

    override fun getChannel(): java.nio.channels.SocketChannel? = inner.channel

    override fun close() {
        inner.close()
    }

    override fun shutdownInput() {
        inner.shutdownInput()
    }

    override fun shutdownOutput() {
        inner.shutdownOutput()
    }

    override fun isConnected(): Boolean = inner.isConnected

    override fun isBound(): Boolean = inner.isBound

    override fun isClosed(): Boolean = inner.isClosed

    override fun isInputShutdown(): Boolean = inner.isInputShutdown

    override fun isOutputShutdown(): Boolean = inner.isOutputShutdown

    override fun setTcpNoDelay(on: Boolean) {
        inner.tcpNoDelay = on
    }

    override fun getTcpNoDelay(): Boolean = inner.tcpNoDelay

    override fun setSoLinger(
        on: Boolean,
        linger: Int,
    ) {
        inner.setSoLinger(on, linger)
    }

    override fun getSoLinger(): Int = inner.soLinger

    override fun sendUrgentData(data: Int) {
        inner.sendUrgentData(data)
    }

    override fun setOOBInline(on: Boolean) {
        inner.oobInline = on
    }

    override fun getOOBInline(): Boolean = inner.oobInline

    override fun setSoTimeout(timeout: Int) {
        inner.soTimeout = timeout
    }

    override fun getSoTimeout(): Int = inner.soTimeout

    override fun setSendBufferSize(size: Int) {
        inner.sendBufferSize = size
    }

    override fun getSendBufferSize(): Int = inner.sendBufferSize

    override fun setReceiveBufferSize(size: Int) {
        inner.receiveBufferSize = size
    }

    override fun getReceiveBufferSize(): Int = inner.receiveBufferSize

    override fun setKeepAlive(on: Boolean) {
        inner.keepAlive = on
    }

    override fun getKeepAlive(): Boolean = inner.keepAlive

    override fun setTrafficClass(tc: Int) {
        inner.trafficClass = tc
    }

    override fun getTrafficClass(): Int = inner.trafficClass

    override fun setReuseAddress(on: Boolean) {
        inner.reuseAddress = on
    }

    override fun getReuseAddress(): Boolean = inner.reuseAddress

    override fun setPerformancePreferences(
        connectionTime: Int,
        latency: Int,
        bandwidth: Int,
    ) {
        inner.setPerformancePreferences(connectionTime, latency, bandwidth)
    }

    // Additional SSLSocket methods

    override fun getHandshakeSession(): SSLSession? = inner.handshakeSession

    override fun getSSLParameters(): javax.net.ssl.SSLParameters = inner.sslParameters

    override fun setSSLParameters(params: javax.net.ssl.SSLParameters?) {
        inner.sslParameters = params
    }

    override fun getApplicationProtocol(): String? = inner.applicationProtocol

    override fun getHandshakeApplicationProtocol(): String? = inner.handshakeApplicationProtocol

    override fun setHandshakeApplicationProtocolSelector(selector: java.util.function.BiFunction<SSLSocket, MutableList<String>, String>?) {
        inner.handshakeApplicationProtocolSelector = selector
    }

    override fun getHandshakeApplicationProtocolSelector(): java.util.function.BiFunction<SSLSocket, MutableList<String>, String>? =
        inner.handshakeApplicationProtocolSelector

    companion object {
        private val LOG = KotlinLogging.logger {}
    }
}
