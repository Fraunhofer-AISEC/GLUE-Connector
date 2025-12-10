package de.fraunhofer.aisec.glue.atls

import kotlinx.coroutines.runBlocking
import java.security.*

/**
 * SignatureSpi implementation that delegates signing operations to the CMC.
 *
 * This class bridges the gap between the Java crypto API to the Hardware-Based key managed by the CMC.
 * This SPI automatically digests all data that is to be signed before delegating the actual signing
 * operation to the CMC.
 */
class CMCSignatureSpi(
    hashAlgorithm: String,
) : SignatureSpi() {

    private val digest: MessageDigest = MessageDigest.getInstance(hashAlgorithm)
    private val hashType: CMCClient.HashType = mapJcaHashToCmcHashType(hashAlgorithm)
    private var cmcPrivateKey: CMCPrivateKey? = null

    /**
     * Initialize this signature object for signing with the given private key.
     */
    override fun engineInitSign(privateKey: PrivateKey) {
        if (privateKey !is CMCPrivateKey) {
            throw InvalidKeyException("Key must be a CMCPrivateKey, got ${privateKey::class.qualifiedName}")
        }
        cmcPrivateKey = privateKey
        digest.reset()
    }

    /**
     * Initialize this signature object for verification with the given public key.
     * This is not supported as CMC only handles signing, not verification.
     */
    override fun engineInitVerify(publicKey: PublicKey) {
        throw UnsupportedOperationException("CMC signature verification is not supported. Use standard Java crypto for verification.")
    }

    /**
     * Update the data to be signed with a single byte.
     */
    override fun engineUpdate(b: Byte) {
        digest.update(b)
    }

    /**
     * Update the data to be signed with a byte array.
     */
    override fun engineUpdate(
        b: ByteArray,
        off: Int,
        len: Int,
    ) {
        digest.update(b, off, len)
    }

    /**
     * Sign the accumulated data using the CMC.
     */
    override fun engineSign(): ByteArray {
        val key = cmcPrivateKey ?: throw SignatureException("Signature not initialized for signing")
        val data = digest.digest()

        return try {
            runBlocking(key.dispatcher) {
                key.cmcClient.tlsSign(data, hashType)
            }
        } catch (e: Exception) {
            throw SignatureException("Failed to sign data with CMC", e)
        } finally {
            digest.reset()
        }
    }

    /**
     * Verify a signature. Not supported as CMC only handles signing.
     */
    override fun engineVerify(sigBytes: ByteArray): Boolean {
        throw UnsupportedOperationException("CMC signature verification is not supported. Use standard Java crypto for verification.")
    }

    /**
     * Set the signature algorithm parameter. Not used for CMC signatures.
     */
    @Deprecated("Use setParameter")
    override fun engineSetParameter(
        param: String?,
        value: Any?,
    ) {
        throw UnsupportedOperationException("Signature parameters are not supported")
    }

    /**
     * Get the signature algorithm parameter. Not used for CMC signatures.
     */
    @Deprecated("Use getParameter")
    override fun engineGetParameter(param: String?): Any {
        throw UnsupportedOperationException("Signature parameters are not supported")
    }

    companion object {
        /**
         * Map JCA hash algorithm names to the hash type values expected by the CMC.
         */
        private fun mapJcaHashToCmcHashType(jcaHash: String): CMCClient.HashType {
            return when (jcaHash.uppercase().replace("-", "")) {
                "SHA1" -> CMCClient.HashType.SHA1
                "SHA224" -> CMCClient.HashType.SHA224
                "SHA256" -> CMCClient.HashType.SHA256
                "SHA384" -> CMCClient.HashType.SHA384
                "SHA512" -> CMCClient.HashType.SHA512
                "SHA3224" -> CMCClient.HashType.SHA3_224
                "SHA3256" -> CMCClient.HashType.SHA3_256
                "SHA3384" -> CMCClient.HashType.SHA3_384
                "SHA3512" -> CMCClient.HashType.SHA3_512
                "MD4" -> CMCClient.HashType.MD4
                "MD5" -> CMCClient.HashType.MD5
                "SHA512/224", "SHA512224" -> CMCClient.HashType.SHA512_224
                "SHA512/256", "SHA512256" -> CMCClient.HashType.SHA512_256
                else -> throw IllegalArgumentException("Unsupported hash algorithm: $jcaHash")
            }
        }
    }
}
