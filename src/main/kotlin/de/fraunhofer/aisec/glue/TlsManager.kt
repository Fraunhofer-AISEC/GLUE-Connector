package de.fraunhofer.aisec.glue

import de.fraunhofer.aisec.glue.atls.CMCClient
import java.security.SecureRandom
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509ExtendedKeyManager
import javax.net.ssl.X509TrustManager

abstract class TlsManager {
    abstract val keyManager: X509ExtendedKeyManager
    abstract val trustManager: X509TrustManager
    abstract val socketFactory: SSLSocketFactory
    abstract val cmcClient: CMCClient

    /**
     * Creates an SSLContext configured with this manager's key and trust managers.
     * Useful for configuring server-side TLS in frameworks like Netty.
     */
    open fun createSSLContext(): SSLContext {
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(arrayOf(keyManager), arrayOf(trustManager), SecureRandom())
        return sslContext
    }
}
