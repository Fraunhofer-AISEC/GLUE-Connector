package de.fraunhofer.aisec.glue.atls

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CoroutineDispatcher
import java.net.ProtocolException
import java.nio.ByteBuffer
import java.security.PEMDecoder
import java.security.cert.X509Certificate
import kotlin.io.encoding.Base64

class CMCClient(
    private val cmcAddress: SocketAddress,
    objectMapper: ObjectMapper,
    dispatcher: CoroutineDispatcher,
    private val selectorManager: SelectorManager = SelectorManager(dispatcher),
) {
    private val objectMapper =
        objectMapper.copy().apply {
            // With the generated classes, we cannot really capture the metadata at the moment, so this is a temporary
            // workaround to just ignore metadata.
            // We will likely want to collect that into an arbitrary map and include it in the attestation report.
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }

    private suspend inline fun <reified T> sendCmcRequest(request: T): Any {
        val serializedRequest = objectMapper.writeValueAsBytes(request)
        val requestSize = serializedRequest.size

        LOG.atDebug {
            message = "Sending CMC request"
            payload = mapOf(
                "requestType" to T::class.simpleName,
                "cmcAddress" to cmcAddress,
            )
        }

        // Despite this method signature indicating support for TCP sockets only, unix sockets also work.
        // I suppose what the authors mean to say is that the socket is stream-oriented.
        aSocket(selectorManager).tcp().connect(cmcAddress).use { socket ->
            val output = socket.openWriteChannel(autoFlush = false)
            val messageType =
                CMC_REQUEST_LOOKUP[T::class] ?: throw IllegalArgumentException(
                    "Unknown payload type ${T::class.qualifiedName}",
                )

            output.writeInt(requestSize)
            output.writeInt(messageType)

            output.writeByteArray(serializedRequest)
            output.flush()

            // Wait for a reply
            val input = socket.openReadChannel()
            val replyLength = input.readInt()
            val replyType = input.readInt()

            val replyPayload = ByteBuffer.allocate(replyLength)
            input.readFully(replyPayload)

            val response = objectMapper.readValue(
                replyPayload.array(),
                CMC_RESPONSE_LOOKUP[replyType]?.java ?: throw ProtocolException("Unknown reply type $replyType"),
            )

            LOG.atDebug {
                message = "Received CMC response"
                payload = mapOf(
                    "requestType" to T::class.simpleName,
                    "replyType" to replyType,
                    "cmcAddress" to cmcAddress,
                )
            }

            return response
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
        when (val resp = sendCmcRequest(req)) {
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
        // TODO: Other fields?
        val encodedNonce = Base64.encode(nonce)
        val req = VerificationRequest(VERSION, encodedNonce, report, metadata, cacheKey, cacheMisses)
        when (val resp = sendCmcRequest(req)) {
            // TODO: Either return full report or define and document when we accept the verification result
            is VerificationResponse -> return resp.result.summary.status == "success"
            is SocketError -> throw IllegalStateException("Received error from CMC: ${resp.msg}")
            else -> throw IllegalStateException("Unexpected response type ${resp::class.qualifiedName}")
        }
    }

    suspend fun fetchPeerCache(cacheKey: String): List<String> {
        val req = PeerCacheRequest(VERSION, cacheKey)
        when (val resp = sendCmcRequest(req)) {
            is PeerCacheResponse -> return resp.cache
            is SocketError -> throw IllegalStateException("Received error from CMC: ${resp.msg}")
            else -> throw IllegalStateException("Unexpected response type ${resp::class.qualifiedName}")
        }
    }

    suspend fun tlsCerts(): List<X509Certificate> {
        val req = TLSCertRequest(VERSION)
        when (val resp = sendCmcRequest(req)) {
            is TLSCertResponse -> {
                val decoder = PEMDecoder.of()
                return resp.certificate.map(Base64::decode).map { pem ->
                    decoder.decode(pem.inputStream(), X509Certificate::class.java)
                }
            }
            is SocketError -> throw IllegalStateException("Received error from CMC: ${resp.msg}")
            else -> throw IllegalStateException("Unexpected response type ${resp::class.qualifiedName}")
        }
    }

    suspend fun tlsSign(
        content: ByteArray,
        hashType: HashType,
    ): ByteArray {
        val req =
            TLSSignRequest(
                VERSION,
                Base64.encode(content),
                hashType.ordinal.toLong(),
                TLSSignRequest.PssOpts(0),
            )
        when (val resp = sendCmcRequest(req)) {
            is TLSSignResponse -> return Base64.decode(resp.signedContent)
            is SocketError -> throw IllegalStateException("Received error from CMC: ${resp.msg}")
            else -> throw IllegalStateException("Unexpected response type ${resp::class.qualifiedName}")
        }
    }

    @Suppress("unused")
    enum class HashType {
        SHA1,
        SHA224,
        SHA256,
        SHA384,
        SHA512,
        MD4,
        MD5,
        MD5SHA1,
        RIPEMD160,
        SHA3_224,
        SHA3_256,
        SHA3_384,
        SHA3_512,
        SHA512_224,
        SHA512_256,
        BLAKE2S_256,
        BLAKE2B_256,
        BLAKE2B_384,
        BLAKE2B_512,
    }

    companion object {
        private const val VERSION = "1.2.0"

        private val CMC_REQUEST_LOOKUP =
            mapOf(
                AttestationRequest::class to 1,
                VerificationRequest::class to 2,
                TLSSignRequest::class to 3,
                TLSCertRequest::class to 4,
                PeerCacheRequest::class to 5,
                MeasureRequest::class to 6,
            )

        private val CMC_RESPONSE_LOOKUP =
            mapOf(
                0 to SocketError::class,
                1 to AttestationResponse::class,
                2 to VerificationResponse::class,
                3 to TLSSignResponse::class,
                4 to TLSCertResponse::class,
                5 to PeerCacheResponse::class,
                6 to MeasureResponse::class,
            )

        private val LOG = KotlinLogging.logger {}
    }
}
