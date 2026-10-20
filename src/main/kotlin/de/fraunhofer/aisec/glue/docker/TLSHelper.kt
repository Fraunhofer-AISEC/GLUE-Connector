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
import javax.net.ssl.X509TrustManager

@Suppress("unused")
object TLSHelper {
    private const val CA_CERT_FILE = "ca.pem"
    private const val CERT_FILE = "cert.pem"
    private const val KEY_FILE = "key.pem"

    fun loadDockerTLSParams(certDir: String): Pair<CertificateAndKey, X509TrustManager> {
        // It seems the java crypto API is unable to load pkcs#1 keys like they are used by pretty much every project
        // I've come across...
        // Until I have some time to think about how to still work with the standard docker certificates, tls will not
        // be supported for the docker client.
        // Since we really only need docker TCP for the ci anyway, this is fine.
        TODO("Docker tls is not implemented yet")
    }
}
