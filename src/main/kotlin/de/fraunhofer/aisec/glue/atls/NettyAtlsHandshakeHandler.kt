package de.fraunhofer.aisec.glue.atls

import com.fasterxml.jackson.databind.ObjectMapper
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.ssl.SslHandler
import io.netty.handler.ssl.SslHandshakeCompletionEvent
import kotlinx.coroutines.CoroutineDispatcher
import java.security.MessageDigest
import java.util.logging.Logger
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
                Logger.getLogger(NettyAtlsHandshakeHandler::class.java.name)
                    .info("TLS handshake completed successfully, initiating ATLS handshake")
                handshakeComplete = true
                performAtlsHandshakeExchange(ctx)
            } else {
                Logger.getLogger(NettyAtlsHandshakeHandler::class.java.name)
                    .warning("TLS handshake failed: ${evt.cause()?.message}")
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

            // Extract the Exporter Channel Binding
            val channelBinding = session.exportKeyingMaterialData("EXPORTER-Channel-Binding", null, 32)

            // Calculate cache key from peer certificate SHA-256 hash
            val cacheKey =
                session.peerCertificates.firstOrNull()?.let {
                    MessageDigest.getInstance("SHA-256").digest(it.encoded).toHexString()
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

            // Start the ATLS handshake and send the initial packet
            val initialPacket = atlsHandshake!!.start()
            sendPacket(ctx, initialPacket)
        } catch (e: Exception) {
            Logger.getLogger(NettyAtlsHandshakeHandler::class.java.name)
                .warning("Failed to initiate ATLS handshake: ${e.message}")
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

        ctx.writeAndFlush(buffer).addListener { future ->
            if (!future.isSuccess) {
                Logger.getLogger(NettyAtlsHandshakeHandler::class.java.name)
                    .warning("Failed to send ATLS packet: ${future.cause()?.message}")
                ctx.close()
            }
        }
    }

    override fun channelRead(
        ctx: ChannelHandlerContext,
        msg: Any,
    ) {
        if (handshakeComplete && !postHandshakeComplete && atlsHandshake != null) {
            // Process ATLS handshake packets
            if (msg is ByteBuf) {
                try {
                    // Accumulate received bytes
                    val bytes = ByteArray(msg.readableBytes())
                    msg.readBytes(bytes)
                    receivedData.add(bytes)

                    // Try to read length-prefixed packet
                    val packet = readPacketIfAvailable()
                    if (packet != null) {
                        // Feed the packet to ATLS handshake
                        val responsePacket = atlsHandshake!!.feedPacket(packet)

                        // Send response if available
                        if (responsePacket != null) {
                            sendPacket(ctx, responsePacket)
                        }

                        // Check if handshake is complete
                        if (!atlsHandshake!!.isRunning) {
                            if (atlsHandshake!!.failure != null) {
                                Logger.getLogger(NettyAtlsHandshakeHandler::class.java.name)
                                    .warning("ATLS handshake failed: ${atlsHandshake!!.failure?.message}")
                                ctx.close()
                            } else {
                                Logger.getLogger(NettyAtlsHandshakeHandler::class.java.name)
                                    .info("ATLS handshake completed successfully")
                                postHandshakeComplete = true
                                atlsHandshake = null
                                receivedData.clear()

                                // Remove this handler from the pipeline as its job is done
                                ctx.pipeline().remove(this)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Logger.getLogger(NettyAtlsHandshakeHandler::class.java.name)
                        .warning("Error processing ATLS packet: ${e.message}")
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

        if (totalSize < 4 + length) {
            return null // Need more data
        }

        // Extract the packet
        val packet = combined.copyOfRange(4, 4 + length)

        // Keep remaining data for next packet
        receivedData.clear()
        if (totalSize > 4 + length) {
            receivedData.add(combined.copyOfRange(4 + length, totalSize))
        }

        return packet
    }

    override fun exceptionCaught(
        ctx: ChannelHandlerContext,
        cause: Throwable,
    ) {
        Logger.getLogger(NettyAtlsHandshakeHandler::class.java.name)
            .warning("Exception in ATLS handshake handler: ${cause.message}")
        ctx.close()
    }
}
