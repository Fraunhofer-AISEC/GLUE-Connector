package de.fraunhofer.aisec.glue.atls

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
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
    private val cacheKey: String?,
    dispatcher: CoroutineDispatcher,
) {
    private val handshakeScope = CoroutineScope(dispatcher)

    var state: State = State.Initial
        private set(value) {
            LOG.atTrace {
                message = "ATLS handshake state transition"
                payload =
                    mapOf(
                        "attestationType" to attestationType.name,
                        "isClient" to isClient,
                        "oldState" to field.name,
                        "newState" to value.name,
                    )
            }
            field = value
        }

    var failure: Throwable? = null
        private set

    val isRunning
        get() = state != State.Completed && state != State.Failed

    private val attestSelf
        get() = attestationType.attestClient && isClient || attestationType.attestServer && !isClient

    private val attestPeer
        get() = attestationType.attestClient && !isClient || attestationType.attestServer && isClient

    /**
     * Start the ATLS handshake state machine, returning the initial packet to send to the peer.
     */
    fun start(): ByteArray {
        if (state != State.Initial) {
            throw IllegalStateException("ATLS handshake state machine is already started")
        }

        LOG.atDebug {
            message = "Starting ATLS handshake state machine"
            payload =
                mapOf(
                    "attestationType" to attestationType.name,
                    "isClient" to isClient,
                    "attestSelf" to attestSelf,
                    "attestPeer" to attestPeer,
                    "hasCacheKey" to (cacheKey != null),
                )
        }

        val cache =
            if (attestPeer && cacheKey != null) {
                LOG.atDebug {
                    message = "Fetching peer cache from CMC"
                    payload = mapOf("cacheKey" to cacheKey)
                }
                runBlocking(handshakeScope.coroutineContext) {
                    cmcClient.fetchPeerCache(cacheKey)
                }.also { result ->
                    LOG.atDebug {
                        message = "Fetched peer cache from CMC"
                        payload =
                            mapOf(
                                "cacheKey" to cacheKey,
                                "cacheSize" to result.size,
                            )
                    }
                }
            } else {
                LOG.atDebug {
                    message = "Skipping peer cache fetch"
                    payload =
                        mapOf(
                            "attestPeer" to attestPeer,
                            "hasCacheKey" to (cacheKey != null),
                        )
                }
                null
            }

        state = State.AwaitingRequest
        val req = AtlsHandshakeRequest(VERSION, attestationType.protocolValue, cache)
        val packet = objectMapper.writeValueAsBytes(req)

        LOG.atDebug {
            message = "ATLS handshake started, created initial request packet"
            payload =
                mapOf(
                    "state" to state.name,
                    "packetSize" to packet.size,
                    "version" to VERSION,
                    "attestationTypeValue" to attestationType.protocolValue,
                )
        }

        return packet
    }

    private inline fun <reified T> parsePacket(packet: ByteArray): T {
        try {
            LOG.atDebug {
                message = "Parsing ATLS packet"
                payload =
                    mapOf(
                        "targetType" to T::class.simpleName,
                        "packetSize" to packet.size,
                    )
            }
            return objectMapper.readValue<T>(packet)
        } catch (e: Exception) {
            LOG.atWarn {
                message = "Failed to parse ATLS packet"
                cause = e
                payload =
                    mapOf(
                        "targetType" to T::class.simpleName,
                        "packetSize" to packet.size,
                    )
            }
            throw IllegalArgumentException("Could not parse packet as a ${T::class.simpleName}", e)
        }
    }

    private fun handleRequest(packet: ByteArray): ByteArray {
        LOG.atDebug {
            message = "Handling ATLS request"
            payload =
                mapOf(
                    "currentState" to state.name,
                    "packetSize" to packet.size,
                )
        }

        val req = parsePacket<AtlsHandshakeRequest>(packet)

        LOG.atDebug {
            message = "Parsed ATLS request"
            payload =
                mapOf(
                    "version" to req.version,
                    "attestValue" to req.attest,
                    "hasCachedData" to (req.cached != null),
                )
        }

        if (req.version != VERSION) {
            LOG.atWarn {
                message = "Unsupported ATLS version"
                payload =
                    mapOf(
                        "receivedVersion" to req.version,
                        "supportedVersion" to VERSION,
                    )
            }
            throw IllegalArgumentException("Version ${req.version} is not supported")
        }

        val peerAttestationType = AttestationType.fromProtocolValue(req.attest)
        LOG.atDebug {
            message = "Checking attestation type compatibility"
            payload =
                mapOf(
                    "ourAttestationType" to attestationType.name,
                    "peerAttestationType" to peerAttestationType.name,
                    "isClient" to isClient,
                )
        }

        if (!attestationType.isCompatible(peerAttestationType, isClient)) {
            LOG.atWarn {
                message = "Incompatible attestation types"
                payload =
                    mapOf(
                        "ourAttestationType" to attestationType.name,
                        "peerAttestationType" to peerAttestationType.name,
                    )
            }
            throw IllegalArgumentException("The peer's attestation type $peerAttestationType is incompatible with $attestationType")
        }

        val cmcResult =
            if (attestSelf) {
                LOG.atDebug {
                    message = "Requesting attestation from CMC"
                    payload = mapOf("hasCachedData" to (req.cached != null))
                }
                runBlocking(handshakeScope.coroutineContext) {
                    cmcClient.attest(channelBinding, req.cached)
                }.also { result ->
                    LOG.atDebug {
                        message = "Received attestation from CMC"
                        payload =
                            mapOf(
                                "hasMetadata" to (result.metadata != null),
                                "cacheMissesCount" to (result.cacheMisses?.size ?: 0),
                            )
                    }
                }
            } else {
                LOG.atDebug {
                    message = "Skipping self-attestation"
                    payload = mapOf("attestSelf" to attestSelf)
                }
                null
            }

        state = State.AwaitingResponse
        val response =
            AtlsHandshakeResponse(
                VERSION,
                report = cmcResult?.report,
                metadata = cmcResult?.metadata,
                cacheMisses = cmcResult?.cacheMisses,
            )
        val responsePacket = objectMapper.writeValueAsBytes(response)

        LOG.atDebug {
            message = "Created ATLS response packet"
            payload =
                mapOf(
                    "state" to state.name,
                    "packetSize" to responsePacket.size,
                    "hasReport" to (response.report != null),
                    "hasMetadata" to (response.metadata != null),
                )
        }

        return responsePacket
    }

    private fun handleResponse(packet: ByteArray): ByteArray {
        LOG.atDebug {
            message = "Handling ATLS response"
            payload =
                mapOf(
                    "currentState" to state.name,
                    "packetSize" to packet.size,
                )
        }

        val req = parsePacket<AtlsHandshakeResponse>(packet)

        LOG.atDebug {
            message = "Parsed ATLS response"
            payload =
                mapOf(
                    "version" to req.version,
                    "hasReport" to (req.report != null),
                    "hasMetadata" to (req.metadata != null),
                    "hasCacheMisses" to (req.cacheMisses != null),
                    "hasError" to (req.error != null),
                )
        }

        if (req.version != VERSION) {
            LOG.atWarn {
                message = "Unsupported ATLS version in response"
                payload =
                    mapOf(
                        "receivedVersion" to req.version,
                        "supportedVersion" to VERSION,
                    )
            }
            throw IllegalArgumentException("Version ${req.version} is not supported")
        }

        // While we don't currently use the error field in the protocol, the peer might ;)
        if (req.error != null) {
            LOG.atWarn {
                message = "Peer reported error in response"
                payload = mapOf("error" to req.error)
            }
            throw IllegalArgumentException("Peer reported an error: ${req.error}")
        }

        val verificationResult =
            if (attestPeer) {
                val report =
                    req.report ?: run {
                        LOG.atWarn {
                            message = "Missing report in attestation response"
                            payload = mapOf("attestPeer" to attestPeer)
                        }
                        throw IllegalArgumentException("Report is missing from attestation response")
                    }
                LOG.atDebug {
                    message = "Verifying peer attestation with CMC"
                    payload =
                        mapOf(
                            "hasReport" to true,
                            "hasMetadata" to (req.metadata != null),
                            "hasCacheKey" to (cacheKey != null),
                            "cacheMissesCount" to (req.cacheMisses?.size ?: 0),
                        )
                }
                runBlocking(
                    handshakeScope.coroutineContext,
                ) { cmcClient.verify(channelBinding, report, req.metadata, cacheKey, req.cacheMisses) }.also { result ->
                    LOG.atDebug {
                        message = "CMC verification completed"
                        payload = mapOf("verificationResult" to result)
                    }
                }
            } else {
                LOG.atDebug {
                    message = "Skipping peer verification"
                    payload = mapOf("attestPeer" to attestPeer)
                }
                true
            }

        // We fail the handshake if the verification failed, but don't throw as we still need to send out the completion packet
        // As a well-behaved caller must check the state, the handshake will still fail.
        if (verificationResult) {
            state = State.AwaitingComplete
        } else {
            state = State.Failed
            failure = IllegalStateException("Peer attestation verification failed")
        }
        val completePacket = objectMapper.writeValueAsBytes(AtlsHandshakeComplete(VERSION, verificationResult))

        LOG.atDebug {
            message = "Created ATLS complete packet"
            payload =
                mapOf(
                    "state" to state.name,
                    "packetSize" to completePacket.size,
                    "success" to verificationResult,
                )
        }

        return completePacket
    }

    private fun handleComplete(packet: ByteArray): ByteArray? {
        LOG.atDebug {
            message = "Handling ATLS complete"
            payload =
                mapOf(
                    "currentState" to state.name,
                    "packetSize" to packet.size,
                )
        }

        val req = parsePacket<AtlsHandshakeComplete>(packet)

        LOG.atDebug {
            message = "Parsed ATLS complete"
            payload =
                mapOf(
                    "version" to req.version,
                    "success" to req.success,
                    "hasError" to (req.error != null),
                )
        }

        if (req.version != VERSION) {
            LOG.atWarn {
                message = "Unsupported ATLS version in complete"
                payload =
                    mapOf(
                        "receivedVersion" to req.version,
                        "supportedVersion" to VERSION,
                    )
            }
            throw IllegalArgumentException("Version ${req.version} is not supported")
        }

        if (req.error != null) {
            LOG.atWarn {
                message = "Peer reported error in complete"
                payload = mapOf("error" to req.error)
            }
            throw IllegalArgumentException("Peer reported an error: ${req.error}")
        }

        state = State.Completed
        LOG.info { "ATLS handshake state machine completed successfully" }
        LOG.atDebug {
            message = "ATLS handshake state machine completed"
            payload =
                mapOf(
                    "finalState" to state.name,
                    "success" to req.success,
                )
        }

        return null
    }

    /**
     * Feed a received packet to the state machine, returning the next packet to send to the peer (if any).
     */
    fun feedPacket(packet: ByteArray): ByteArray? {
        LOG.atDebug {
            message = "Feeding packet to ATLS state machine"
            payload =
                mapOf(
                    "currentState" to state.name,
                    "packetSize" to packet.size,
                )
        }

        try {
            val result =
                when (state) {
                    State.Initial -> throw IllegalStateException("ATLS handshake state machine was not started yet")
                    State.AwaitingRequest -> handleRequest(packet)
                    State.AwaitingResponse -> handleResponse(packet)
                    State.AwaitingComplete -> handleComplete(packet)
                    State.Completed, State.Failed -> throw IllegalStateException("ATLS handshake state machine has already completed")
                }

            LOG.atDebug {
                message = "State machine processed packet"
                payload =
                    mapOf(
                        "newState" to state.name,
                        "hasResponsePacket" to (result != null),
                        "responseSize" to (result?.size ?: 0),
                    )
            }

            return result
        } catch (e: Exception) {
            LOG.atWarn {
                message = "ATLS handshake state machine encountered error"
                cause = e
                payload =
                    mapOf(
                        "currentState" to state.name,
                        "hadPreviousFailure" to (failure != null),
                    )
            }

            state = State.Failed
            if (failure != null) {
                // We've already failed previously, so don't overwrite the original failure reason
                LOG.atDebug {
                    message = "Already failed previously, not overwriting failure"
                    payload = mapOf("originalFailure" to failure!!.message)
                }
                return null
            }
            failure = e

            // If the exchange failed because the peer sent an error completion packet, we don't send one ourselves
            val errorPacket = runCatching { objectMapper.readValue<AtlsHandshakeComplete>(packet) }.getOrNull()
            if (errorPacket?.success == false) {
                failure = IllegalStateException("Peer terminated the handshake with an error: ${errorPacket.error}")
                LOG.atWarn {
                    message = "Peer terminated handshake with error"
                    payload = mapOf("peerError" to errorPacket.error)
                }
                return null
            } else {
                val errorResponse =
                    objectMapper.writeValueAsBytes(
                        AtlsHandshakeComplete(
                            VERSION,
                            success = false,
                            error = e.message,
                        ),
                    )
                LOG.atDebug {
                    message = "Sending error completion packet to peer"
                    payload =
                        mapOf(
                            "errorMessage" to e.message,
                            "packetSize" to errorResponse.size,
                        )
                }
                return errorResponse
            }
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
        private val LOG = KotlinLogging.logger {}
    }
}
