/*
 * Copyright 2025-2026 Fraunhofer AISEC
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

import kotlinx.coroutines.CoroutineDispatcher
import java.security.PrivateKey
import java.security.cert.X509Certificate

/**
 * A PrivateKey marker implementation for the CMC-managed key.
 *
 * The actual private key never leaves the CMC - all cryptographic operations
 * are delegated to the CMC through the custom security provider.
 *
 * @property cmcClient The CMC client to use for signing operations
 * @property certificate The X509 certificate associated with this private key
 * @property dispatcher The coroutine dispatcher to use for blocking CMC calls
 */
class CMCPrivateKey(
    internal val cmcClient: CMCClient,
    private val certificate: X509Certificate,
    internal val dispatcher: CoroutineDispatcher,
    internal val keyId: String,
) : PrivateKey {
    /**
     * Returns the algorithm name from the associated certificate's public key.
     */
    override fun getAlgorithm(): String = certificate.publicKey.algorithm

    /**
     * Returns null since the key is managed by the CMC and never exposed.
     */
    override fun getFormat(): String? = null

    /**
     * Returns null since the key is managed by the CMC and never exposed.
     */
    override fun getEncoded(): ByteArray? = null

    override fun toString(): String = "CMCPrivateKey(algorithm=$algorithm)"
}
