package de.fraunhofer.aisec.glue.atls

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.security.PrivateKey
import java.security.cert.X509Certificate

/**
 * A PrivateKey marker implementation for the CMC-managed key.
 *
 * The actual private key never leaves the CMC - all cryptographic operations
 * are delegated to the CMC through the custom security provider.
 *
 * @property cmcClient The CMC client to use for signing operations
 * @property certificate The X509 certificate associated with this private key
 * @property dispatcher The coroutine dispatcher to use for blocking CMC calls
 */
class CMCPrivateKey(
    internal val cmcClient: CMCClient,
    private val certificate: X509Certificate,
    internal val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : PrivateKey {
    /**
     * Returns the algorithm name from the associated certificate's public key.
     */
    override fun getAlgorithm(): String = certificate.publicKey.algorithm

    /**
     * Returns null since the key is managed by the CMC and never exposed.
     */
    override fun getFormat(): String? = null

    /**
     * Returns null since the key is managed by the CMC and never exposed.
     */
    override fun getEncoded(): ByteArray? = null

    override fun toString(): String = "CMCPrivateKey(algorithm=$algorithm)"
}
