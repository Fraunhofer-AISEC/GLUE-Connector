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

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CoroutineDispatcher
import java.net.ProtocolException
import java.net.SocketException
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

        LOG.atTrace {
            message = "Sending CMC request"
            payload =
                mapOf(
                    "requestType" to T::class.simpleName,
                    "cmcAddress" to cmcAddress,
                    "serializedRequest" to serializedRequest.decodeToString(),
                )
        }

        // Despite this method signature indicating support for TCP sockets only, unix sockets also work.
        // I suppose what the authors mean to say is that the socket is stream-oriented.
        try {
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

                val response =
                    objectMapper.readValue(
                        replyPayload.array(),
                        CMC_RESPONSE_LOOKUP[replyType]?.java
                            ?: throw ProtocolException("Unknown reply type $replyType"),
                    )

                LOG.atTrace {
                    message = "Received CMC response"
                    payload =
                        mapOf(
                            "requestType" to T::class.simpleName,
                            "replyType" to CMC_RESPONSE_LOOKUP[replyType]?.simpleName,
                            "cmcAddress" to cmcAddress,
                            "serializedReply" to replyPayload.array().decodeToString(),
                        )
                }

                return response
            }
        } catch (e: SocketException) {
            LOG.atError {
                message = "Failed to communicate with the CMC"
                cause = e
                payload =
                    mapOf(
                        "requestType" to T::class.simpleName,
                        "cmcAddress" to cmcAddress,
                    )
            }

            throw e
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
        val req = AttestationRequest(VERSION, nonce = encodedNonce, cached = cached)
        when (val resp = sendCmcRequest(req)) {
            is AttestationResponse -> return CMCReport(resp.report!!, null, null)
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
        metadata: Map<String, Any>?,
        cacheMisses: List<String>? = null,
        peer: String? = null,
    ): Boolean {
        val encodedNonce = Base64.encode(nonce)
        val req = VerificationRequest(VERSION, nonce = encodedNonce, report = report, peer = peer)
        when (val resp = sendCmcRequest(req)) {
            is VerificationResponse -> return resp.result?.summary?.status == "success"
            is SocketError -> throw IllegalStateException("Received error from CMC: ${resp.msg}")
            else -> throw IllegalStateException("Unexpected response type ${resp::class.qualifiedName}")
        }
    }

    suspend fun fetchPeerCache(cacheKey: String): List<String> {
        val req = PeerCacheRequest(VERSION, peer = cacheKey)
        when (val resp = sendCmcRequest(req)) {
            is PeerCacheResponse -> return resp.cache ?: emptyList()
            is SocketError -> throw IllegalStateException("Received error from CMC: ${resp.msg}")
            else -> throw IllegalStateException("Unexpected response type ${resp::class.qualifiedName}")
        }
    }

    suspend fun tlsKey(
        cn: String,
        dnsNames: List<String>,
        ipAddresses: List<String>,
        type: TLSKeyType = TLSKeyType.SOFTWARE,
        alg: TLSKeyAlg = TLSKeyAlg.EC256,
    ): String {
        val req =
            TLSCreateRequest(
                VERSION,
                keyConfig =
                    TLSCreateRequest.KeyConfig(
                        type = type.representation,
                        alg = alg.representation,
                        cn = cn,
                        dnsNames = dnsNames,
                        ipAddresses = ipAddresses,
                    ),
            )
        return when (val resp = sendCmcRequest(req)) {
            is TLSCreateResponse -> resp.keyId ?: throw IllegalStateException("CMC did not return a key ID")
            is SocketError -> throw IllegalStateException("Received error from CMC: ${resp.msg}")
            else -> throw IllegalStateException("Unexpected response type ${resp::class.qualifiedName}")
        }
    }

    suspend fun tlsCerts(keyId: String? = null): List<X509Certificate> {
        val req = TLSCertRequest(VERSION, keyId = keyId)
        when (val resp = sendCmcRequest(req)) {
            is TLSCertResponse -> {
                val decoder = PEMDecoder.of()
                return resp.certificate?.map(Base64::decode)?.map { pem ->
                    decoder.decode(pem.inputStream(), X509Certificate::class.java)
                } ?: emptyList()
            }

            is SocketError -> {
                throw IllegalStateException("Received error from CMC: ${resp.msg}")
            }

            else -> {
                throw IllegalStateException("Unexpected response type ${resp::class.qualifiedName}")
            }
        }
    }

    suspend fun tlsSign(
        content: ByteArray,
        hashType: HashType,
        keyId: String? = null,
    ): ByteArray {
        val req =
            TLSSignRequest(
                VERSION,
                keyId = keyId,
                content = Base64.encode(content),
                hashAlg = hashType.string,
            )
        when (val resp = sendCmcRequest(req)) {
            is TLSSignResponse -> return Base64.decode(
                resp.signedContent ?: throw IllegalStateException("No signed data received from CMC"),
            )

            is SocketError -> throw IllegalStateException("Received error from CMC: ${resp.msg}")

            else -> throw IllegalStateException("Unexpected response type ${resp::class.qualifiedName}")
        }
    }

    // The following types map to the equivalent values in the CMC.
    // Unused values should therefore not be a warning.

    @Suppress("unused")
    enum class HashType(
        val string: String,
    ) {
        SHA1("SHA-1"),
        SHA224("SHA-224"),
        SHA256("SHA-256"),
        SHA384("SHA-384"),
        SHA512("SHA-512"),
        MD4("MD4"),
        MD5("MD5"),
        MD5SHA1("MD5+SHA1"),
        RIPEMD160("RIPEMD-160"),
        SHA3_224("SHA3-224"),
        SHA3_256("SHA3-256"),
        SHA3_384("SHA3-384"),
        SHA3_512("SHA3-512"),
        SHA512_224("SHA-512/224"),
        SHA512_256("SHA-512/256"),
        BLAKE2S_256("BLAKE2S-256"),
        BLAKE2B_256("BLAKE2B-256"),
        BLAKE2B_384("BLAKE2B-384"),
        BLAKE2B_512("BLAKE2B-512"),
    }

    @Suppress("unused")
    enum class TLSKeyType(
        val representation: TLSCreateRequest.Type,
    ) {
        TPM(TLSCreateRequest.Type.tpm),
        SOFTWARE(TLSCreateRequest.Type.sw),
        SNP(TLSCreateRequest.Type.snp),
    }

    @Suppress("unused")
    enum class TLSKeyAlg(
        val representation: TLSCreateRequest.Alg,
    ) {
        EC256(TLSCreateRequest.Alg.EC256),
        EC384(TLSCreateRequest.Alg.EC384),
        EC521(TLSCreateRequest.Alg.EC521),
        RSA2048(TLSCreateRequest.Alg.RSA2048),
        RSA4096(TLSCreateRequest.Alg.RSA4096),
    }

    companion object {
        private const val VERSION = "1.4.0"

        private val CMC_REQUEST_LOOKUP =
            mapOf(
                AttestationRequest::class to 1,
                VerificationRequest::class to 2,
                TLSCreateRequest::class to 3,
                TLSSignRequest::class to 4,
                TLSCertRequest::class to 5,
                PeerCacheRequest::class to 6,
                MeasureRequest::class to 7,
            )

        private val CMC_RESPONSE_LOOKUP =
            mapOf(
                0 to SocketError::class,
                1 to AttestationResponse::class,
                2 to VerificationResponse::class,
                3 to TLSCreateResponse::class,
                4 to TLSSignResponse::class,
                5 to TLSCertResponse::class,
                6 to PeerCacheResponse::class,
                7 to MeasureResponse::class,
            )

        private val LOG = KotlinLogging.logger {}
    }
}
