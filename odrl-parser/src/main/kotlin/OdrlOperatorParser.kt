package fhg.glue.odrl

import com.fasterxml.jackson.databind.JsonNode

class OdrlOperatorParser: OdrlParser() {
    override val key: String = "operator"

    fun parse(root: JsonNode): OdrlOperator {
        if (!root.isTextual)
            throw OdrlException("Expected operator to be a string")

        val operator = root.asText()

        return OdrlOperator(operator)
    }
}