package fhg.glue.odrl

import com.fasterxml.jackson.databind.JsonNode

class OdrlActionParser: OdrlParser() {
    override val key = "action"

    fun parse(root: JsonNode): OdrlAction {
        if (root.isTextual) {
            isIri(root.asText())
            return OdrlAction(root.asText())
        }

        if (!root.isObject)
            throw OdrlException("Expected action to be a string or object")

        val uid = getStringOrThrow(root, "@id")
        isIri(uid)


        return OdrlAction(uid)
    }
}