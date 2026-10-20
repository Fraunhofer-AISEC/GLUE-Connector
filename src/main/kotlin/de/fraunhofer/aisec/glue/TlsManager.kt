/*
 * Copyright 2025 Fraunhofer AISEC
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
