package de.fraunhofer.aisec.glue.docker

import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

object TLSHelper {
    private const val CA_CERT_FILE = "ca.pem"
    private const val CERT_FILE = "cert.pem"
    private const val KEY_FILE = "key.pem"

    fun loadDockerTLSParams(certDir: String): Pair<SSLContext, X509TrustManager> {
        // It seems the java crypto API is unable to load pkcs#1 keys like they are used by pretty much every project
        // I've come across...
        // Until I have some time to think about how to still work with the standard docker certificates, tls will not
        // be supported for the docker client.
        // Since we really only need docker TCP for the ci anyway, this is fine.
        TODO("Docker tls is not implemented yet")
    }
}
