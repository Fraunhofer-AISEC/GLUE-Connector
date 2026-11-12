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
package de.fraunhofer.aisec.glue.pipeline.atls

import com.fasterxml.jackson.annotation.*
import de.fraunhofer.aisec.glue.Utils.truncate
import de.fraunhofer.iosb.odrl.glue.GlueAtlsProxyEndpoint
import de.fraunhofer.iosb.odrl.glue.GlueParameter
import de.fraunhofer.iosb.odrl.glue.GlueValue

/**
 * Configuration parameter for the special ATLS proxy container
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = AtlsParameter::class)
@JsonTypeName("http://fhg.de/glue/internal/AtlsProxyConfiguration")
class AtlsParameter(
    @param:JsonProperty("http://fhg.de/glue/enabled")
    val enabled: Boolean,
    @param:JsonProperty("http://fhg.de/glue/mode")
    val mode: String,
    @param:JsonProperty("http://fhg.de/glue/cmcAddress")
    val cmcAddress: String,
    @param:JsonProperty("http://fhg.de/glue/caCertificates")
    val caCertificate: String,
    @param:JsonProperty("http://fhg.de/glue/proxyEndpoint")
    @param:JsonSetter(nulls = Nulls.AS_EMPTY)
    val proxyEndpoint: List<GlueAtlsProxyEndpoint>? = null,
    @param:JsonProperty("http://fhg.de/glue/keyId")
    val keyId: String,
    @param:JsonProperty("http://fhg.de/glue/attest")
    val attest: String,
    @param:JsonProperty("http://fhg.de/glue/mtls")
    val mtls: Boolean,
    value: GlueValue? = null,
) : GlueParameter(value) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as AtlsParameter

        if (enabled != other.enabled) return false
        if (mode != other.mode) return false
        if (cmcAddress != other.cmcAddress) return false
        if (caCertificate != other.caCertificate) return false
        if (proxyEndpoint != other.proxyEndpoint) return false
        if (keyId != other.keyId) return false
        if (attest != other.attest) return false
        if (mtls != other.mtls) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + mode.hashCode()
        result = 31 * result + cmcAddress.hashCode()
        result = 31 * result + caCertificate.hashCode()
        result = 31 * result + proxyEndpoint.hashCode()
        result = 31 * result + keyId.hashCode()
        result = 31 * result + attest.hashCode()
        result = 31 * result + mtls.hashCode()
        return result
    }

    override fun toString(): String =
        "AtlsParameter(mode=$mode, cmcAddress=$cmcAddress, caCertificates=${
            caCertificate.truncate(
                16,
            )
        }, proxyConnections=$proxyEndpoint, keyId=$keyId, attest=$attest, mtls=$mtls)"
}
