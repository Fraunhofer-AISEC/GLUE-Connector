package de.fraunhofer.iosb.odrl.glue

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.JsonNode
import de.fraunhofer.iosb.odrl.parser.JsonLd

@JsonLd(GlueIdentifiers.CONTEXT)
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
data class GlueValue(@JsonValue val jsonNode: JsonNode) {
    companion object {
        @JsonCreator @JvmStatic fun create(node: JsonNode): GlueValue {
            return GlueValue(node)
        }
    }
}