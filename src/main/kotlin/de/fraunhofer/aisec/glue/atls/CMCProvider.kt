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

import java.security.Provider

/**
 * Java Security Provider for the TLS signing functionality offered by the CMC.
 * This class is meant to be used in conjunction with the [CMCKeyManager] class, which exposes the private key and
 * certificates held in the CMC to the Java crypto API.
 *
 * The provider must be registered before creating SSLContext instances that use CMCKeyManager.
 */
class CMCProvider : Provider(NAME, VERSION_STRING, INFO) {
    init {
        // Register signature algorithms for each hash type and asymmetric algorithm combination
        // Format: "HashAlgorithmWithAsymmetricAlgorithm" (e.g., "SHA256withRSA")

        // RSA signatures
        putService(CMCSignatureService(this, "SHA1withRSA", "SHA-1"))
        putService(CMCSignatureService(this, "SHA224withRSA", "SHA-224"))
        putService(CMCSignatureService(this, "SHA256withRSA", "SHA-256"))
        putService(CMCSignatureService(this, "SHA384withRSA", "SHA-384"))
        putService(CMCSignatureService(this, "SHA512withRSA", "SHA-512"))
        putService(CMCSignatureService(this, "SHA3-224withRSA", "SHA3-224"))
        putService(CMCSignatureService(this, "SHA3-256withRSA", "SHA3-256"))
        putService(CMCSignatureService(this, "SHA3-384withRSA", "SHA3-384"))
        putService(CMCSignatureService(this, "SHA3-512withRSA", "SHA3-512"))
        putService(CMCSignatureService(this, "MD5withRSA", "MD5"))

        // ECDSA signatures
        putService(CMCSignatureService(this, "SHA1withECDSA", "SHA-1"))
        putService(CMCSignatureService(this, "SHA224withECDSA", "SHA-224"))
        putService(CMCSignatureService(this, "SHA256withECDSA", "SHA-256"))
        putService(CMCSignatureService(this, "SHA384withECDSA", "SHA-384"))
        putService(CMCSignatureService(this, "SHA512withECDSA", "SHA-512"))
        putService(CMCSignatureService(this, "SHA3-224withECDSA", "SHA3-224"))
        putService(CMCSignatureService(this, "SHA3-256withECDSA", "SHA3-256"))
        putService(CMCSignatureService(this, "SHA3-384withECDSA", "SHA3-384"))
        putService(CMCSignatureService(this, "SHA3-512withECDSA", "SHA3-512"))

        // DSA signatures
        putService(CMCSignatureService(this, "SHA1withDSA", "SHA-1"))
        putService(CMCSignatureService(this, "SHA224withDSA", "SHA-224"))
        putService(CMCSignatureService(this, "SHA256withDSA", "SHA-256"))
        putService(CMCSignatureService(this, "SHA384withDSA", "SHA-384"))
        putService(CMCSignatureService(this, "SHA512withDSA", "SHA-512"))
        putService(CMCSignatureService(this, "SHA3-224withDSA", "SHA3-224"))
        putService(CMCSignatureService(this, "SHA3-256withDSA", "SHA3-256"))
        putService(CMCSignatureService(this, "SHA3-384withDSA", "SHA3-384"))
        putService(CMCSignatureService(this, "SHA3-512withDSA", "SHA3-512"))

        // EdDSA signatures (Ed25519 and Ed448 use different hash functions internally,
        // but we map them to SHA512 as the closest CMC equivalent for the signature operation)
        putService(CMCSignatureService(this, "Ed25519", "SHA-512"))
        putService(CMCSignatureService(this, "Ed448", "SHA-512"))
        putService(CMCSignatureService(this, "EdDSA", "SHA-512"))

        // RSASSA-PSS (RSA Probabilistic Signature Scheme)
        putService(CMCSignatureService(this, "SHA256withRSAandMGF1", "SHA-256"))
        putService(CMCSignatureService(this, "SHA384withRSAandMGF1", "SHA-384"))
        putService(CMCSignatureService(this, "SHA512withRSAandMGF1", "SHA-512"))
        putService(CMCSignatureService(this, "RSASSA-PSS", "SHA-256"))
        // Default to SHA256

        // Additional hash algorithms
        putService(CMCSignatureService(this, "SHA512-224withRSA", "SHA-512/224"))
        putService(CMCSignatureService(this, "SHA512-256withRSA", "SHA-512/256"))
        putService(CMCSignatureService(this, "SHA512-224withECDSA", "SHA-512/224"))
        putService(CMCSignatureService(this, "SHA512-256withECDSA", "SHA-512/256"))
    }

    private class CMCSignatureService(
        provider: Provider,
        algorithm: String,
        private val hashAlgorithm: String,
    ) : Service(provider, "Signature", algorithm, CMCSignatureSpi::class.java.name, null, null) {
        override fun newInstance(constructorParameter: Any?): Any = CMCSignatureSpi(hashAlgorithm)
    }

    companion object {
        private const val NAME = "CMC"
        private const val VERSION_STRING = "1.0"
        private const val INFO = "CMC Security Provider - Delegates cryptographic operations to the CMC"
    }
}
