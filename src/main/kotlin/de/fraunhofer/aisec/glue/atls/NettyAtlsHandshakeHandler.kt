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
import io.ktor.server.config.configLoaders
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.ssl.SslHandler
import io.netty.handler.ssl.SslHandshakeCompletionEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.MessageDigest
import javax.net.ssl.ExtendedSSLSession
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Handler that performs custom data exchange immediately after TLS handshake completion.
 * This handler intercepts the connection after SSL/TLS negotiation and before any HTTP traffic.
 */
class NettyAtlsHandshakeHandler(
    private val objectMapper: ObjectMapper,
    private val cmcClient: CMCClient,
) : ChannelInboundHandlerAdapter() {
    private var handshakeComplete = false
    private var handshakeJob: Job? = null

    // The coroutine and Netty callbacks both run on this channel's event loop.
    private val dataAvailable = Channel<Unit>(Channel.CONFLATED)
    private val receivedData = mutableListOf<ByteArray>()

    override fun userEventTriggered(
        ctx: ChannelHandlerContext,
        evt: Any,
    ) {
        if (evt is SslHandshakeCompletionEvent) {
            if (evt.isSuccess) {
                LOG.atInfo {
                    message = "TLS handshake completed successfully, starting ATLS post-handshake"
                    payload =
                        mapOf(
                            "channel" to ctx.channel().id().asShortText(),
                            "remoteAddress" to ctx.channel().remoteAddress().toString(),
                        )
                }
                handshakeComplete = true
                handshakeJob =
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            performAtlsHandshakeExchange(ctx)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            LOG.atWarn {
                                message = "Failed to perform ATLS handshake"
                                cause = e
                                payload = mapOf("channel" to ctx.channel().id().asShortText())
                            }
                            ctx.close()
                        }
                    }
            } else {
                LOG.atWarn {
                    message = "TLS handshake failed"
                    cause = evt.cause()
                    payload =
                        mapOf(
                            "channel" to ctx.channel().id().asShortText(),
                            "remoteAddress" to ctx.channel().remoteAddress().toString(),
                        )
                }
                ctx.close()
            }
        }
        ctx.fireUserEventTriggered(evt)
    }

    private suspend fun performAtlsHandshakeExchange(ctx: ChannelHandlerContext) {
        val session =
            ctx
                .pipeline()
                .get(SslHandler::class.java)
                .engine()
                .session as ExtendedSSLSession

        val (clientBinding, serverBinding) = AtlsHandshake.createClientServerBindings(session)
        val cacheKey =
            session.peerCertificates.firstOrNull()?.let {
                MessageDigest.getInstance("SHA-256").digest(it.encoded).toHexString()
            }

        val handshake =
            AtlsHandshake(
                objectMapper,
                serverBinding,
                clientBinding,
                cmcClient,
                AtlsHandshake.AttestationType.MUTUAL,
                false,
                cacheKey,
            )

        sendPacket(ctx, handshake.start())
        while (handshake.isRunning) {
            handshake.feedPacket(receivePacket(ctx))?.let { sendPacket(ctx, it) }
        }

        if (handshake.failure != null) {
            LOG.atWarn {
                message = "ATLS handshake failed"
                cause = handshake.failure
                payload = mapOf("channel" to ctx.channel().id().asShortText())
            }
            ctx.close()
            return
        }

        // Bytes after the final ATLS packet belong to the HTTP pipeline.
        if (receivedData.isNotEmpty()) {
            val buffer = ctx.alloc().buffer(receivedData.sumOf { it.size })
            receivedData.forEach { buffer.writeBytes(it) }
            receivedData.clear()
            ctx.fireChannelRead(buffer)
        }

        ctx.pipeline().remove(this)
        ctx.read()
    }

    private suspend fun sendPacket(
        ctx: ChannelHandlerContext,
        packet: ByteArray,
    ) {
        // Send length-prefixed packet (4 bytes length + data)
        val buffer = ctx.alloc().buffer(4 + packet.size)
        buffer.writeInt(packet.size)
        buffer.writeBytes(packet)

        LOG.atDebug {
            message = "Sending ATLS packet"
            payload =
                mapOf(
                    "channel" to ctx.channel().id().asShortText(),
                    "packetSize" to packet.size,
                    "totalBufferSize" to (4 + packet.size),
                )
        }

        suspendCancellableCoroutine { continuation ->
            ctx.writeAndFlush(buffer).addListener { future ->
                if (future.isSuccess) {
                    continuation.resume(Unit)
                } else {
                    continuation.resumeWithException(future.cause())
                }
            }
        }
    }

    private suspend fun receivePacket(ctx: ChannelHandlerContext): ByteArray {
        while (true) {
            readPacketIfAvailable()?.let { return it }
            // Discard the signal for bytes already inspected before waiting for more.
            dataAvailable.tryReceive()
            ctx.read()
            dataAvailable.receive()
        }
    }

    override fun channelRead(
        ctx: ChannelHandlerContext,
        msg: Any,
    ) {
        if (!handshakeComplete || msg !is ByteBuf) {
            ctx.fireChannelRead(msg)
            return
        }

        try {
            if (!msg.isReadable) {
                throw IllegalStateException("Received empty buffer")
            }
            val bytes = ByteArray(msg.readableBytes())
            msg.readBytes(bytes)
            receivedData.add(bytes)
            dataAvailable.trySend(Unit)
        } catch (e: Exception) {
            LOG.atWarn {
                message = "Error receiving ATLS packet"
                cause = e
                payload = mapOf("channel" to ctx.channel().id().asShortText())
            }
            ctx.close()
        } finally {
            msg.release()
        }
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        handshakeJob?.cancel()
        ctx.fireChannelInactive()
    }

    override fun handlerRemoved(ctx: ChannelHandlerContext) {
        handshakeJob?.cancel()
    }

    private fun readPacketIfAvailable(): ByteArray? {
        // Combine all received data
        val totalSize = receivedData.sumOf { it.size }
        if (totalSize < 4) {
            LOG.atWarn {
                message = "Insufficient data for packet length prefix"
                payload =
                    mapOf(
                        "bufferedBytes" to totalSize,
                        "requiredBytes" to 4,
                    )
            }
            return null // Need at least 4 bytes for length
        }

        val combined = ByteArray(totalSize)
        var offset = 0
        for (chunk in receivedData) {
            chunk.copyInto(combined, offset)
            offset += chunk.size
        }

        // Read the length prefix
        val length =
            ((combined[0].toInt() and 0xFF) shl 24) or
                ((combined[1].toInt() and 0xFF) shl 16) or
                ((combined[2].toInt() and 0xFF) shl 8) or
                (combined[3].toInt() and 0xFF)

        LOG.atDebug {
            message = "Read packet length prefix"
            payload =
                mapOf(
                    "expectedPacketSize" to length,
                    "bufferedBytes" to totalSize,
                    "hasCompletePacket" to (totalSize >= 4 + length),
                )
        }

        if (totalSize < 4 + length) {
            return null // Need more data
        }

        // Extract the packet
        val packet = combined.copyOfRange(4, 4 + length)

        // Keep remaining data for next packet
        receivedData.clear()
        if (totalSize > 4 + length) {
            val remainingSize = totalSize - (4 + length)
            receivedData.add(combined.copyOfRange(4 + length, totalSize))
            LOG.atDebug {
                message = "Extracted packet with remaining data"
                payload =
                    mapOf(
                        "packetSize" to length,
                        "remainingBuffered" to remainingSize,
                    )
            }
        } else {
            LOG.atDebug {
                message = "Extracted complete packet"
                payload =
                    mapOf(
                        "packetSize" to length,
                    )
            }
        }

        return packet
    }

    override fun exceptionCaught(
        ctx: ChannelHandlerContext,
        cause: Throwable,
    ) {
        LOG.atWarn {
            message = "Exception in ATLS handshake handler"
            this.cause = cause
            payload =
                mapOf(
                    "channel" to ctx.channel().id().asShortText(),
                    "handshakeComplete" to handshakeComplete,
                )
        }
        ctx.close()
    }

    companion object {
        private val LOG = KotlinLogging.logger {}
    }
}
