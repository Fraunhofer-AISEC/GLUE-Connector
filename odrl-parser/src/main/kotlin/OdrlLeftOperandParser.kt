package fhg.glue.odrl

import com.fasterxml.jackson.databind.JsonNode

class OdrlLeftOperandParser : OdrlParser() {
    override val key: String = "leftOperand"

    fun parse(root: JsonNode): OdrlLeftOperand {
        if (!root.isTextual)
            throw OdrlException("Expected left operand to be a string")

        val uid = root.asText()

        return OdrlLeftOperand(uid)
    }
}