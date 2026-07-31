package de.fraunhofer.iosb.odrl.glue

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import java.net.URI

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = GlueRestParameter::class)
@JsonTypeName(GlueIdentifiers.REST_PARAMETER)
class GlueRestParameter(
    // Properties of superclass GlueParameter
    value: GlueValue? = null,

    // Properties of GlueRestParameter
    @param:JsonProperty(GlueIdentifiers.HTTP_URL)
    val httpUrl: URI,

    @param:JsonProperty(GlueIdentifiers.HTTP_TYPE)
    val httpType: GlueHttpType = GlueHttpType.GET,
): GlueParameter(value) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as GlueRestParameter

        if (httpUrl != other.httpUrl) return false
        if (httpType != other.httpType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + httpUrl.hashCode()
        result = 31 * result + httpType.hashCode()
        return result
    }

    override fun toString(): String {
        return "GlueRestParameter(httpUrl=$httpUrl, httpType=$httpType, value=$value)"
    }
}