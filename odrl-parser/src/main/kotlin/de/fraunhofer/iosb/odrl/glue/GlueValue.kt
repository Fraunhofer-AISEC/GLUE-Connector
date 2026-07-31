package de.fraunhofer.iosb.odrl.glue

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.databind.JsonNode
import de.fraunhofer.iosb.odrl.OdrlRightOperand

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = GlueValue::class)
@JsonTypeName(GlueIdentifiers.VALUE)
data class GlueValue(
    @param:JsonProperty(GlueIdentifiers.JSON)
    val json: JsonNode
): OdrlRightOperand() {
    override fun compareTo(other: OdrlRightOperand): Int {
        throw UnsupportedOperationException("Cannot compare GlueValue to ${other.javaClass.simpleName}")
    }
}