package fhg.glue.odrl

import com.fasterxml.jackson.databind.JsonNode

class OdrlActionParser: OdrlParser() {
    override val key = "action"

    fun parse(root: JsonNode): OdrlAction {
        if (!root.isTextual)
            throw OdrlException("Expected action to be a string")

        val name = root.asText()


        return OdrlAction(name)
    }
}