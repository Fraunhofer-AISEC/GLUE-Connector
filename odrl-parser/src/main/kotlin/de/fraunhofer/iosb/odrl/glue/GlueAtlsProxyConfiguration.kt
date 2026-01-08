package de.fraunhofer.iosb.odrl.glue

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.annotation.Nulls
import de.fraunhofer.iosb.odrl.parser.JsonLd

@JsonLd(GlueIdentifiers.CONTEXT)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = GlueAtlsProxyConfiguration::class)
@JsonTypeName(GlueIdentifiers.ATLS_PROXY_CONFIGURATION)
data class GlueAtlsProxyConfiguration(
    @param:JsonProperty(GlueIdentifiers.PROXY_MODE)
    val proxyMode: String,
    @param:JsonProperty(GlueIdentifiers.PROXY_ENDPOINT)
    @param:JsonSetter(nulls = Nulls.AS_EMPTY)
    val proxyEndpoint: List<GlueAtlsProxyEndpoint>? = null,
)
