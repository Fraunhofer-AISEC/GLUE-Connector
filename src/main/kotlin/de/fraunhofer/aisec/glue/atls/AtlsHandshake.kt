package de.fraunhofer.aisec.glue.atls

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

/**
 * This class provides the handshake state machine for the ATLS protocol.
 * While communication with the CMC is handled internally, the caller must perform all other IO and synchronisation.
 */
class AtlsHandshake(
    private val objectMapper: ObjectMapper,
    private val channelBinding: ByteArray,
    private val cmcClient: CMCClient,
    private val attestationType: AttestationType,
    private val isClient: Boolean,
    dispatcher: CoroutineDispatcher,
) {
    private val handshakeScope = CoroutineScope(dispatcher)

    var state: State = State.Initial
        private set

    val isRunning
        get() = state != State.Completed && state != State.Failed

    /**
     * Start the ATLS handshake state machine, returning the initial packet to send to the peer.
     */
    fun start(): ByteArray {
        if (state != State.Initial) {
            throw IllegalStateException("ATLS handshake state machine is already started")
        }

        state = State.AwaitingRequest
        val req = AtlsHandshakeRequest(VERSION, attestationType.protocolValue)
        return objectMapper.writeValueAsBytes(req)
    }

    private inline fun <reified T> parsePacket(packet: ByteArray): T {
        try {
            return objectMapper.readValue<T>(packet)
        } catch (e: Exception) {
            throw IllegalArgumentException("Could not parse packet as a ${T::class.simpleName}", e)
        }
    }

    private fun handleRequest(packet: ByteArray): ByteArray {
        val req = parsePacket<AtlsHandshakeRequest>(packet)
        if (req.version != VERSION) {
            throw IllegalArgumentException("Version ${req.version} is not supported")
        }

        val peerAttestationType = AttestationType.fromProtocolValue(req.attest)
        if (!attestationType.isCompatible(peerAttestationType, isClient)) {
            throw IllegalArgumentException("The peer's attestation type $peerAttestationType is incompatible with $attestationType")
        }

        val report =
            if (isClient && attestationType.attestClient || !isClient && attestationType.attestServer) {
                runBlocking(handshakeScope.coroutineContext) {
                    cmcClient.attest(channelBinding, req.cached)
                }
            } else {
                null
            }

        state = State.AwaitingResponse
        return objectMapper.writeValueAsBytes(AtlsHandshakeResponse(VERSION, report = report))
    }

    private fun handleResponse(packet: ByteArray): ByteArray {
        val req = parsePacket<AtlsHandshakeResponse>(packet)
        if (req.version != VERSION) {
            throw IllegalArgumentException("Version ${req.version} is not supported")
        }

        // While we don't currently use the error field in the protocol, the peer might ;)
        if (req.error != null) {
            throw IllegalArgumentException("Peer reported an error: ${req.error}")
        }

        val verificationResult =
            if (isClient && attestationType.attestServer || !isClient && attestationType.attestClient) {
                val report = req.report ?: throw IllegalArgumentException("Report is missing from attestation response")
                runBlocking(handshakeScope.coroutineContext) { cmcClient.verify(channelBinding, report) }
            } else {
                false
            }

        state = State.AwaitingComplete
        return objectMapper.writeValueAsBytes(AtlsHandshakeComplete(VERSION, verificationResult))
    }

    private fun handleComplete(packet: ByteArray): ByteArray? {
        val req = parsePacket<AtlsHandshakeComplete>(packet)
        if (req.version != VERSION) {
            throw IllegalArgumentException("Version ${req.version} is not supported")
        }

        if (req.error != null) {
            throw IllegalArgumentException("Peer reported an error: ${req.error}")
        }

        state = State.Completed
        return null
    }

    /**
     * Feed a received packet to the state machine, returning the next packet to send to the peer (if any).
     */
    fun feedPacket(packet: ByteArray): ByteArray? {
        try {
            return when (state) {
                State.Initial -> throw IllegalStateException("ATLS handshake state machine was not started yet")
                State.AwaitingRequest -> handleRequest(packet)
                State.AwaitingResponse -> handleResponse(packet)
                State.AwaitingComplete -> handleComplete(packet)
                State.Completed, State.Failed -> throw IllegalStateException("ATLS handshake state machine has already completed")
            }
        } catch (e: Exception) {
            state = State.Failed
            throw e
        }
    }

    enum class AttestationType(internal val protocolValue: Int) {
        MUTUAL(0),
        CLIENT(1),
        SERVER(2),
        NONE(3),
        ;

        val attestClient
            get() = this == MUTUAL || this == CLIENT

        val attestServer
            get() = this == MUTUAL || this == SERVER

        internal fun isCompatible(
            other: AttestationType,
            isClient: Boolean,
        ): Boolean {
            // Check if the other side provides what we need
            return if (isClient) {
                // We're the client - check if the server attests when we need it
                !this.attestServer || other.attestServer
            } else {
                // We're the server - check if the client attests when we need it
                !this.attestClient || other.attestClient
            }
        }

        companion object {
            internal fun fromProtocolValue(value: Int): AttestationType {
                return when (value) {
                    0 -> MUTUAL
                    1 -> CLIENT
                    2 -> SERVER
                    3 -> NONE
                    else -> throw IllegalArgumentException("Unknown attestation type $value")
                }
            }
        }
    }

    enum class State {
        Initial,
        AwaitingRequest,
        AwaitingResponse,
        AwaitingComplete,
        Completed,
        Failed,
    }

    companion object {
        const val VERSION = "1.0.0"
    }
}
