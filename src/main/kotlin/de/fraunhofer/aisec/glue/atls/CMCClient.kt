package de.fraunhofer.aisec.glue.atls

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import java.net.ProtocolException
import java.nio.ByteBuffer
import kotlin.io.encoding.Base64
import kotlin.reflect.KClass

class CMCClient(
    private val cmcAddress: SocketAddress,
    objectMapper: ObjectMapper,
    private val selectorManager: SelectorManager = SelectorManager(Dispatchers.IO),
) {
    private val objectMapper =
        objectMapper.copy().apply {
            // With the generated classes, we cannot really capture the metadata at the moment, so this is a temporary
            // workaround to just ignore metadata.
            // We will likely want to collect that into an arbitrary map and include it in the attestation report.
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }

    private suspend inline fun <reified T> sendCmcRequest(payload: T): Any {
        val serializedPayload = objectMapper.writeValueAsBytes(payload)
        val payloadLength = serializedPayload.size

        // Despite this method signature indicating support for TCP sockets only, unix sockets also work.
        // I suppose what the authors mean to say is that the socket is stream-oriented.
        return aSocket(selectorManager).tcp().connect(cmcAddress).use { socket ->
            val output = socket.openWriteChannel(autoFlush = true)

            output.writeInt(payloadLength)
            output.writeInt(
                CMC_REQUEST_LOOKUP[T::class] ?: throw IllegalArgumentException("Unknown payload type ${T::class.qualifiedName}"),
            )

            output.writeByteArray(serializedPayload)
            output.flush()

            // Wait for a reply
            val input = socket.openReadChannel()
            val replyLength = input.readInt()
            val replyType = input.readInt()

            val replyPayload = ByteBuffer.allocate(replyLength)
            input.readFully(replyPayload)

            objectMapper.readValue(
                replyPayload.array(),
                CMC_RESPONSE_LOOKUP[replyType]?.java ?: throw ProtocolException("Unknown reply type $replyType"),
            )
        }
    }

    /**
     * Perform attestation using the CMC.
     * The attestation report will contain the provided nonce for verification.
     */
    suspend fun attest(
        nonce: ByteArray,
        cached: List<String>? = null,
    ): CMCReport {
        val encodedNonce = Base64.encode(nonce)
        val req = AttestationRequest(VERSION, encodedNonce, cached)
        val resp = sendCmcRequest(req)
        when (resp) {
            is AttestationResponse -> return CMCReport(resp.report, resp.metadata, resp.cacheMisses)
            is SocketError -> throw IllegalStateException("Received error from CMC: ${resp.msg}")
            else -> throw IllegalStateException("Unexpected response type ${resp::class.qualifiedName}")
        }
    }

    /**
     * Verify a CMC attestation report.
     */
    suspend fun verify(
        nonce: ByteArray,
        report: String,
        metadata: MetadataMap? = null,
        cacheKey: String? = null,
        cacheMisses: List<String>? = null,
    ): Boolean {
        val encodedNonce = Base64.encode(nonce)
        val req = VerificationRequest(VERSION, encodedNonce, report, metadata, cacheKey, cacheMisses)
        val resp = sendCmcRequest(req)
        when (resp) {
            is VerificationResponse -> return resp.result.summary.status == "success"
            is SocketError -> throw IllegalStateException("Received error from CMC: ${resp.msg}")
            else -> throw IllegalStateException("Unexpected response type ${resp::class.qualifiedName}")
        }
    }

    suspend fun fetchPeerCache(cacheKey: String): List<String>? {
        val req = PeerCacheRequest(VERSION, cacheKey)
        val resp = sendCmcRequest(req)
        when (resp) {
            is PeerCacheResponse -> return resp.cache
            is SocketError -> throw IllegalStateException("Received error from CMC: ${resp.msg}")
            else -> throw IllegalStateException("Unexpected response type ${resp::class.qualifiedName}")
        }
    }

    companion object {
        private const val VERSION = "1.2.0"

        private val CMC_REQUEST_LOOKUP =
            mapOf<KClass<*>, Int>(
                AttestationRequest::class to 1,
                VerificationRequest::class to 2,
                TLSSignRequest::class to 3,
                TLSCertRequest::class to 4,
                PeerCacheRequest::class to 5,
                MeasureRequest::class to 6,
            )

        private val CMC_RESPONSE_LOOKUP =
            mapOf<Int, KClass<*>>(
                0 to SocketError::class,
                1 to AttestationResponse::class,
                2 to VerificationResponse::class,
                3 to TLSSignResponse::class,
                4 to TLSCertResponse::class,
                5 to PeerCacheResponse::class,
                6 to MeasureResponse::class,
            )
    }
}
