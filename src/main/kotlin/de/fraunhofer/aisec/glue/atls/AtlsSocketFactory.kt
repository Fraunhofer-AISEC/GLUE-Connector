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
package de.fraunhofer.aisec.glue.atls

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.CoroutineDispatcher
import java.net.InetAddress
import java.net.Socket
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

class AtlsSocketFactory(
    private val objectMapper: ObjectMapper,
    private val cmcClient: CMCClient,
    private val dispatcher: CoroutineDispatcher,
    inner: SSLSocketFactory?,
) : SSLSocketFactory() {
    private val inner: SSLSocketFactory = inner ?: (getDefault() as SSLSocketFactory)

    override fun getDefaultCipherSuites(): Array<out String?>? = inner.defaultCipherSuites

    override fun getSupportedCipherSuites(): Array<out String?>? = inner.supportedCipherSuites

    override fun createSocket(
        s: Socket?,
        host: String?,
        port: Int,
        autoClose: Boolean,
    ): Socket = AtlsSocket(inner.createSocket(s, host, port, autoClose) as SSLSocket, objectMapper, cmcClient, dispatcher)

    override fun createSocket(
        host: String?,
        port: Int,
    ): Socket = AtlsSocket(inner.createSocket(host, port) as SSLSocket, objectMapper, cmcClient, dispatcher)

    override fun createSocket(
        host: String?,
        port: Int,
        localHost: InetAddress?,
        localPort: Int,
    ): Socket =
        AtlsSocket(
            inner.createSocket(host, port, localHost, localPort) as SSLSocket,
            objectMapper,
            cmcClient,
            dispatcher,
        )

    override fun createSocket(
        host: InetAddress?,
        port: Int,
    ): Socket = AtlsSocket(inner.createSocket(host, port) as SSLSocket, objectMapper, cmcClient, dispatcher)

    override fun createSocket(
        address: InetAddress?,
        port: Int,
        localAddress: InetAddress?,
        localPort: Int,
    ): Socket =
        AtlsSocket(
            inner.createSocket(address, port, localAddress, localPort) as SSLSocket,
            objectMapper,
            cmcClient,
            dispatcher,
        )
}
