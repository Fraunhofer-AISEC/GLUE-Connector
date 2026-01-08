package de.fraunhofer.iosb.odrl.glue

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import de.fraunhofer.iosb.odrl.parser.JsonLd

@JsonLd(GlueIdentifiers.CONTEXT)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = GlueAtlsProxyEndpoint::class)
@JsonTypeName(GlueIdentifiers.ATLS_PROXY_ENDPOINT)
data class GlueAtlsProxyEndpoint(
    @param:JsonProperty(GlueIdentifiers.LISTEN_ENDPOINT)
    val listenEndpoint: String,
    @param:JsonProperty(GlueIdentifiers.NEXT_ENDPOINT)
    val nextEndpoint: String,
)
