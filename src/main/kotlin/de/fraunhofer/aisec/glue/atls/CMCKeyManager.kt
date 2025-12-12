package de.fraunhofer.aisec.glue.atls

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import java.net.Socket
import java.security.Principal
import java.security.PrivateKey
import java.security.cert.X509Certificate
import javax.net.ssl.SSLEngine
import javax.net.ssl.X509ExtendedKeyManager

/**
 * An X509ExtendedKeyManager implementation that uses the CMC for certificate and key management.
 *
 * This class provides a Java-conforming KeyManager interface while delegating all
 * cryptographic operations to the CMC. Certificates are fetched from the CMC and cached
 * on first access.
 *
 * @property cmcClient The CMC client to use for certificate and signing operations
 * @property dispatcher The coroutine dispatcher to use for blocking CMC calls (defaults to Dispatchers.IO)
 */
class CMCKeyManager(
    private val cmcClient: CMCClient,
    private val dispatcher: CoroutineDispatcher,
) : X509ExtendedKeyManager() {
    private val certificateChain: Array<X509Certificate> by lazy {
        runBlocking(dispatcher) {
            cmcClient.tlsCerts().toTypedArray()
        }
    }

    private val privateKey: CMCPrivateKey by lazy {
        CMCPrivateKey(cmcClient, certificateChain[0], dispatcher)
    }

    /**
     * Returns the single alias used by this key manager.
     */
    override fun chooseClientAlias(
        keyType: Array<out String>?,
        issuers: Array<out Principal>?,
        socket: Socket?,
    ): String? {
        // Check if our certificate's key type matches any of the requested types
        if (keyType != null && certificateChain[0].publicKey.algorithm !in keyType) {
            return null
        }
        return ALIAS
    }

    /**
     * Returns the single alias used by this key manager.
     */
    override fun chooseServerAlias(
        keyType: String?,
        issuers: Array<out Principal>?,
        socket: Socket?,
    ): String? {
        // Check if our certificate's key type matches the requested type
        if (keyType != null && certificateChain[0].publicKey.algorithm != keyType) {
            return null
        }
        return ALIAS
    }

    /**
     * Returns the single alias used by this key manager (SSLEngine variant).
     */
    override fun chooseEngineClientAlias(
        keyType: Array<out String>?,
        issuers: Array<out Principal>?,
        engine: SSLEngine?,
    ): String? {
        // Check if our certificate's key type matches any of the requested types
        if (keyType != null && certificateChain[0].publicKey.algorithm !in keyType) {
            return null
        }
        return ALIAS
    }

    /**
     * Returns the single alias used by this key manager (SSLEngine variant).
     */
    override fun chooseEngineServerAlias(
        keyType: String?,
        issuers: Array<out Principal>?,
        engine: SSLEngine?,
    ): String? {
        // Check if our certificate's key type matches the requested type
        if (keyType != null && certificateChain[0].publicKey.algorithm != keyType) {
            return null
        }
        return ALIAS
    }

    /**
     * Returns the certificate chain for the given alias.
     */
    override fun getCertificateChain(alias: String?): Array<X509Certificate>? {
        return if (alias == ALIAS) certificateChain else null
    }

    /**
     * Returns client aliases matching the given key type and issuers.
     */
    override fun getClientAliases(
        keyType: String?,
        issuers: Array<out Principal>?,
    ): Array<String>? {
        // Check if our certificate's key type matches the requested type
        if (keyType != null && certificateChain[0].publicKey.algorithm != keyType) {
            return null
        }
        return arrayOf(ALIAS)
    }

    /**
     * Returns the private key for the given alias.
     */
    override fun getPrivateKey(alias: String?): PrivateKey? {
        return if (alias == ALIAS) privateKey else null
    }

    /**
     * Returns server aliases matching the given key type and issuers.
     */
    override fun getServerAliases(
        keyType: String?,
        issuers: Array<out Principal>?,
    ): Array<String>? {
        // Check if our certificate's key type matches the requested type
        if (keyType != null && certificateChain[0].publicKey.algorithm != keyType) {
            return null
        }
        return arrayOf(ALIAS)
    }

    companion object {
        private const val ALIAS = "cmc"
    }
}
