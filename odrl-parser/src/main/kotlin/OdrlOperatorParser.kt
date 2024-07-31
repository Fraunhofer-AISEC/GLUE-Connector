package fhg.glue.odrl

import com.fasterxml.jackson.databind.JsonNode

class OdrlOperatorParser: OdrlParser() {
    override val key: String = "operator"

    fun parse(root: JsonNode): OdrlOperator {
        if (root.isTextual)
            return OdrlOperator(root.asText())

        if (!root.isObject)
            throw OdrlException("Expected operator to be a string or object")

        return OdrlOperator(getStringOrThrow(root, "@id"))
    }
}