package de.fraunhofer.aisec.glue.atls

import com.fasterxml.jackson.databind.ObjectMapper
import de.fraunhofer.aisec.glue.TlsManager
import de.fraunhofer.aisec.glue.config.ConfigManager
import de.fraunhofer.aisec.glue.pipeline.atls.AtlsConfig
import kotlinx.coroutines.Dispatchers
import java.io.EOFException
import java.io.File
import java.security.KeyStore
import java.security.PEMDecoder
import java.security.SecureRandom
import java.security.Security
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509ExtendedKeyManager
import javax.net.ssl.X509TrustManager

class AtlsManager(
    configManager: ConfigManager,
    objectMapper: ObjectMapper,
) : TlsManager() {
    private val atlsConfig = configManager.getConfig(::AtlsConfig)
    private val cmcClient = CMCClient(atlsConfig.cmcAddress, objectMapper)

    override val keyManager: X509ExtendedKeyManager by lazy {
        Security.addProvider(CMCProvider())
        CMCKeyManager(cmcClient)
    }

    override val trustManager: X509TrustManager by lazy {
        val decoder = PEMDecoder.of()
        val caCerts =
            File(atlsConfig.caCertPath).inputStream().use { certStream ->
                generateSequence {
                    try {
                        decoder.decode(certStream, X509Certificate::class.java)
                    } catch (_: EOFException) {
                        null
                    }
                }
            }

        val certStore = KeyStore.getInstance(KeyStore.getDefaultType())
        certStore.load(null)
        caCerts.forEachIndexed { index, cert -> certStore.setCertificateEntry("ca-$index", cert) }

        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(certStore)

        trustManagerFactory.trustManagers.filterIsInstance<X509TrustManager>().first()
    }

    override val socketFactory: SSLSocketFactory by lazy {
        val tlsContext = SSLContext.getInstance("TLS")
        tlsContext.init(arrayOf(keyManager), arrayOf(trustManager), SecureRandom())
        AtlsSocketFactory(objectMapper, cmcClient, Dispatchers.IO, tlsContext.socketFactory)
    }
}
