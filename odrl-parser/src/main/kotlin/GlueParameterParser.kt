package fhg.glue.odrl

import com.fasterxml.jackson.databind.JsonNode

class GlueParameterParser : OdrlParser() {
    override val key: String = "glue-parameter"

    fun parse(root: JsonNode): GlueParameter {
        if (!root.isObject) {
            throw OdrlException("Expected GLUE parameter to be an object!")
        }

        val type = getStringOrThrow(root, "@type")

        if (type == GlueOdrlIdentifiers.REST_PARAMETER) {
            return GlueRestParameterParser().parse(root);
        }

        throw OdrlException("Invalid GLUE parameter type: $type")
    }
}