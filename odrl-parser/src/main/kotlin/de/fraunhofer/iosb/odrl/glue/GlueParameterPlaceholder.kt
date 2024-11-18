package fhg.glue.odrl.de.fraunhofer.iosb.odrl.glue

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import fhg.glue.odrl.de.fraunhofer.iosb.odrl.parser.JsonLd

@JsonLd(GlueIdentifiers.CONTEXT)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = GlueParameterPlaceholder::class)
@JsonTypeName(GlueIdentifiers.PARAMETER_PLACEHOLDER)
data class GlueParameterPlaceholder(
    @JsonProperty(GlueIdentifiers.KEY)
    val key: String,
)