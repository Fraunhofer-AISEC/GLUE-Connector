package fhg.glue.odrl

import com.fasterxml.jackson.databind.JsonNode

class OdrlLeftOperandParser : OdrlParser() {
    override val key: String = "leftOperand"

    fun parse(root: JsonNode): OdrlLeftOperand {
        if (root.isTextual)
            return OdrlLeftOperand(root.asText())

        if (!root.isObject)
            throw OdrlException("Expected left operand to be a string or object")

        return OdrlLeftOperand(getStringOrThrow(root, "@id"))
    }
}