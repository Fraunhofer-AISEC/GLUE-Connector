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

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + mode.hashCode()
        result = 31 * result + cmcAddress.hashCode()
        result = 31 * result + caCertificate.hashCode()
        result = 31 * result + proxyEndpoint.hashCode()
        result = 31 * result + keyId.hashCode()
        return result
    }

    override fun toString(): String {
        return "AtlsParameter(mode=$mode, cmcAddress=$cmcAddress, caCertificates=${
            caCertificate.truncate(
                16,
            )
        }, proxyConnections=$proxyEndpoint), keyId=$keyId"
    }
}
