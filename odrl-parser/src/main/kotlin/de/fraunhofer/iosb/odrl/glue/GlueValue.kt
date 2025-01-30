package de.fraunhofer.iosb.odrl.glue

import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.databind.JsonNode
import de.fraunhofer.iosb.odrl.*
import de.fraunhofer.iosb.odrl.parser.JsonLd

@JsonLd(GlueIdentifiers.CONTEXT)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = GlueValue::class)
@JsonTypeName(GlueIdentifiers.VALUE)
data class GlueValue(
    @JsonProperty(GlueIdentifiers.JSON)
    val json: JsonNode
): OdrlRightOperand() {
    override fun compareTo(other: OdrlRightOperand): Int {
        throw UnsupportedOperationException("Cannot compare GlueValue to ${other.javaClass.simpleName}")
    }
}