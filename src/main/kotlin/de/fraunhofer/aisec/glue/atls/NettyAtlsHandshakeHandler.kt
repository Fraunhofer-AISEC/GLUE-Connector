package de.fraunhofer.aisec.glue.atls

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.ssl.SslHandler
import io.netty.handler.ssl.SslHandshakeCompletionEvent
import kotlinx.coroutines.CoroutineDispatcher
import java.security.MessageDigest
import javax.net.ssl.ExtendedSSLSession

/**
 * Handler that performs custom data exchange immediately after TLS handshake completion.
 * This handler intercepts the connection after SSL/TLS negotiation and before any HTTP traffic.
 */
class NettyAtlsHandshakeHandler(
    private val objectMapper: ObjectMapper,
    private val cmcClient: CMCClient,
    private val dispatcher: CoroutineDispatcher,
) : ChannelInboundHandlerAdapter() {
    private var handshakeComplete = false
    private var postHandshakeComplete = false
    private var atlsHandshake: AtlsHandshake? = null
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
                performAtlsHandshakeExchange(ctx)
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

    private fun performAtlsHandshakeExchange(ctx: ChannelHandlerContext) {
        try {
            // Get the SSL handler to access the session
            val sslHandler = ctx.pipeline().get(SslHandler::class.java)
            val session = sslHandler.engine().session as ExtendedSSLSession

            LOG.atDebug {
                message = "Extracting SSL session information"
                payload =
                    mapOf(
                        "channel" to ctx.channel().id().asShortText(),
                        "protocol" to session.protocol,
                        "cipherSuite" to session.cipherSuite,
                    )
            }

            // Extract the Exporter Channel Binding
            val channelBinding = session.exportKeyingMaterialData("EXPORTER-Channel-Binding", null, 32)

            LOG.atDebug {
                message = "Extracted channel binding data"
                payload =
                    mapOf(
                        "channel" to ctx.channel().id().asShortText(),
                        "bindingLength" to channelBinding.size,
                    )
            }

            // Calculate cache key from peer certificate SHA-256 hash
            val cacheKey =
                session.peerCertificates.firstOrNull()?.let {
                    MessageDigest.getInstance("SHA-256").digest(it.encoded).toHexString()
                }

            LOG.atDebug {
                message = "Calculated peer certificate cache key"
                payload =
                    mapOf(
                        "channel" to ctx.channel().id().asShortText(),
                        "cacheKey" to (cacheKey ?: "null"),
                        "hasPeerCert" to (session.peerCertificates.firstOrNull() != null),
                    )
            }

            // Create the ATLS handshake with channel binding
            atlsHandshake =
                AtlsHandshake(
                    objectMapper,
                    channelBinding,
                    cmcClient,
                    AtlsHandshake.AttestationType.SERVER,
                    false,
                    cacheKey,
                    dispatcher,
                )

            LOG.atDebug {
                message = "Created ATLS handshake instance"
                payload =
                    mapOf(
                        "channel" to ctx.channel().id().asShortText(),
                        "attestationType" to "SERVER",
                        "useCaching" to false,
                    )
            }

            // Start the ATLS handshake and send the initial packet
            val initialPacket = atlsHandshake!!.start()
            LOG.atDebug {
                message = "Starting ATLS handshake, sending initial packet"
                payload =
                    mapOf(
                        "channel" to ctx.channel().id().asShortText(),
                        "packetSize" to initialPacket.size,
                    )
            }
            sendPacket(ctx, initialPacket)
        } catch (e: Exception) {
            LOG.atWarn {
                message = "Failed to initiate ATLS handshake"
                cause = e
                payload =
                    mapOf(
                        "channel" to ctx.channel().id().asShortText(),
                    )
            }
            ctx.close()
        }
    }

    private fun sendPacket(
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

        ctx.writeAndFlush(buffer).addListener { future ->
            if (!future.isSuccess) {
                LOG.atWarn {
                    message = "Failed to send ATLS packet"
                    cause = future.cause()
                    payload =
                        mapOf(
                            "channel" to ctx.channel().id().asShortText(),
                            "packetSize" to packet.size,
                        )
                }
                ctx.close()
            } else {
                LOG.atDebug {
                    message = "ATLS packet sent successfully"
                    payload =
                        mapOf(
                            "channel" to ctx.channel().id().asShortText(),
                            "packetSize" to packet.size,
                        )
                }
                ctx.read()
            }
        }
    }

    override fun channelRead(
        ctx: ChannelHandlerContext,
        msg: Any,
    ) {
        LOG.info { "Received message of type ${msg.javaClass.simpleName}" }

        if (handshakeComplete && !postHandshakeComplete && atlsHandshake != null) {
            // Process ATLS handshake packets
            if (msg is ByteBuf) {
                try {
                    if (msg.readableBytes() == 0) {
                        throw IllegalStateException("Received empty buffer")
                    }
                    // Accumulate received bytes
                    val bytes = ByteArray(msg.readableBytes())
                    msg.readBytes(bytes)
                    receivedData.add(bytes)

                    LOG.atDebug {
                        message = "Received data during ATLS handshake"
                        payload =
                            mapOf(
                                "channel" to ctx.channel().id().asShortText(),
                                "bytesReceived" to bytes.size,
                                "totalBuffered" to receivedData.sumOf { it.size },
                            )
                    }

                    // Try to read all length-prefixed packets
                    var packet = readPacketIfAvailable()
                    while (packet != null) {
                        LOG.atDebug {
                            message = "Complete ATLS packet received, feeding to handshake"
                            payload =
                                mapOf(
                                    "channel" to ctx.channel().id().asShortText(),
                                    "packetSize" to packet!!.size,
                                )
                        }

                        // Feed the packet to ATLS handshake
                        val responsePacket = atlsHandshake!!.feedPacket(packet)

                        // Send response if available
                        if (responsePacket != null) {
                            LOG.atDebug {
                                message = "ATLS handshake produced response packet"
                                payload =
                                    mapOf(
                                        "channel" to ctx.channel().id().asShortText(),
                                        "responseSize" to responsePacket.size,
                                    )
                            }
                            sendPacket(ctx, responsePacket)
                        } else {
                            LOG.atDebug {
                                message = "ATLS handshake did not produce response packet"
                                payload =
                                    mapOf(
                                        "channel" to ctx.channel().id().asShortText(),
                                    )
                            }
                            // Request more data since we're waiting for a response
                            ctx.read()
                        }

                        // Check if handshake is complete
                        if (!atlsHandshake!!.isRunning) {
                            if (atlsHandshake!!.failure != null) {
                                LOG.atWarn {
                                    message = "ATLS handshake failed"
                                    cause = atlsHandshake!!.failure
                                    payload =
                                        mapOf(
                                            "channel" to ctx.channel().id().asShortText(),
                                        )
                                }
                                ctx.close()
                            } else {
                                LOG.atDebug {
                                    message = "ATLS post-handshake completed successfully, removing handler from pipeline"
                                    payload =
                                        mapOf(
                                            "channel" to ctx.channel().id().asShortText(),
                                            "bufferedDataRemaining" to receivedData.sumOf { it.size },
                                        )
                                }
                                postHandshakeComplete = true
                                atlsHandshake = null

                                // Forward any remaining buffered data to the next handler
                                if (receivedData.isNotEmpty()) {
                                    val remaining = receivedData.sumOf { it.size }
                                    val buffer = ctx.alloc().buffer(remaining)
                                    receivedData.forEach { buffer.writeBytes(it) }
                                    LOG.atDebug {
                                        message = "Forwarding remaining buffered data to next handler"
                                        payload =
                                            mapOf(
                                                "channel" to ctx.channel().id().asShortText(),
                                                "remainingBytes" to remaining,
                                            )
                                    }
                                    ctx.fireChannelRead(buffer)
                                }
                                receivedData.clear()

                                // Remove this handler from the pipeline as its job is done
                                ctx.pipeline().remove(this)
                                ctx.read()
                            }
                            break
                        }

                        // Try to read the next packet
                        packet = readPacketIfAvailable()
                    }
                } catch (e: Exception) {
                    LOG.atWarn {
                        message = "Error processing ATLS packet"
                        cause = e
                        payload =
                            mapOf(
                                "channel" to ctx.channel().id().asShortText(),
                            )
                    }
                    ctx.close()
                } finally {
                    msg.release()
                }
            } else {
                // Not a ByteBuf, pass it along
                ctx.fireChannelRead(msg)
            }
        } else {
            // Normal traffic, pass it through
            ctx.fireChannelRead(msg)
        }
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
                    "postHandshakeComplete" to postHandshakeComplete,
                )
        }
        ctx.close()
    }

    companion object {
        private val LOG = KotlinLogging.logger {}
    }
}
