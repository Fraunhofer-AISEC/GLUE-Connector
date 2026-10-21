/*
 * Copyright 2024-2025 Fraunhofer AISEC
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
package de.fraunhofer.aisec.glue.docker

import io.ktor.network.tls.*
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyStore
import java.security.PEMDecoder
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

object TLSHelper {
    private const val CA_CERT_FILE = "ca.pem"
    private const val CERT_FILE = "cert.pem"
    private const val KEY_FILE = "key.pem"

    fun loadDockerTLSParams(certDir: String): Pair<CertificateAndKey, X509TrustManager> {
        val directory = Path.of(certDir)
        val clientCertificates = readCertificates(directory.resolve(CERT_FILE))
        val caCertificates = readCertificates(directory.resolve(CA_CERT_FILE))
        val privateKey = PEMDecoder.of().decode(Files.readString(directory.resolve(KEY_FILE)), PrivateKey::class.java)

        val trustStore =
            KeyStore.getInstance("PKCS12").apply {
                load(null, null)
                caCertificates.forEachIndexed { index, certificate -> setCertificateEntry("docker-ca-$index", certificate) }
            }
        val trustManager =
            TrustManagerFactory
                .getInstance(TrustManagerFactory.getDefaultAlgorithm())
                .apply { init(trustStore) }
                .trustManagers
                .filterIsInstance<X509TrustManager>()
                .firstOrNull() ?: throw IllegalStateException("Could not create an X509 trust manager for docker")

        return CertificateAndKey(clientCertificates, privateKey) to trustManager
    }

    private fun readCertificates(path: Path): Array<X509Certificate> {
        val certificates =
            Files.newInputStream(path).use { input ->
                CertificateFactory.getInstance("X.509").generateCertificates(input).map { it as X509Certificate }
            }
        require(certificates.isNotEmpty()) { "No certificates found in $path" }
        return certificates.toTypedArray()
    }
}
