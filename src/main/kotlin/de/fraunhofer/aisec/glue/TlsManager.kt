package de.fraunhofer.aisec.glue

import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509ExtendedKeyManager
import javax.net.ssl.X509TrustManager

abstract class TlsManager {
    abstract val keyManager: X509ExtendedKeyManager
    abstract val trustManager: X509TrustManager
    abstract val socketFactory: SSLSocketFactory
}
