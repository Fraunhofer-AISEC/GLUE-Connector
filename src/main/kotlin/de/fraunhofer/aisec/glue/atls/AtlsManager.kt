package de.fraunhofer.aisec.glue.atls

import com.fasterxml.jackson.databind.ObjectMapper
import de.fraunhofer.aisec.glue.TlsManager
import de.fraunhofer.aisec.glue.config.ConfigManager
import de.fraunhofer.aisec.glue.config.ConnectorConfig
import de.fraunhofer.aisec.glue.pipeline.IPv4Address
import de.fraunhofer.aisec.glue.pipeline.atls.AtlsConfig
import io.ktor.network.sockets.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import java.io.EOFException
import java.io.File
import java.security.KeyStore
import java.security.PEMDecoder
import java.security.SecureRandom
import java.security.Security
import java.security.cert.X509Certificate
import javax.net.ssl.*

class AtlsManager(
    configManager: ConfigManager,
    private val objectMapper: ObjectMapper,
    private val dispatcher: CoroutineDispatcher,
    override val cmcClient: CMCClient,
) : TlsManager() {
    private val atlsConfig = configManager.getConfig(::AtlsConfig)
    private val connectorConfig = configManager.getConfig(::ConnectorConfig)

    override val keyManager: X509ExtendedKeyManager by lazy {
        Security.addProvider(CMCProvider())
        val keyId = runBlocking(dispatcher) {
            val hostnameIsIp = runCatching { IPv4Address.parse(connectorConfig.publicUrl.host) }.isSuccess
            val (dnsNames, ipAddresses) =
                if (hostnameIsIp) {
                    Pair(listOf<String>(), listOf(connectorConfig.publicUrl.host))
                } else {
                    Pair(listOf(connectorConfig.publicUrl.host), listOf())
                }
            cmcClient.tlsKey(connectorConfig.publicUrl.host, dnsNames, ipAddresses)
        }
        CMCKeyManager(cmcClient, dispatcher, keyId)
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
                }.toList()
            }

        val certStore = KeyStore.getInstance(KeyStore.getDefaultType())
        certStore.load(null)
        caCerts.forEachIndexed { index, cert -> certStore.setCertificateEntry("ca-$index", cert) }

        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(certStore)

        trustManagerFactory.trustManagers.filterIsInstance<X509TrustManager>().first()
    }

    val sslContext: SSLContext by lazy {
        SSLContext.getInstance("TLS").apply {
            init(arrayOf(keyManager), arrayOf(trustManager), SecureRandom())
        }
    }

    override val socketFactory: SSLSocketFactory by lazy {
        AtlsSocketFactory(objectMapper, cmcClient, dispatcher, sslContext.socketFactory)
    }
}
