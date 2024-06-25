package fhg.glue.odrl

import com.fasterxml.jackson.databind.JsonNode

class RdfTypeParser : OdrlParser() {
    override val key: String = "rdf-type"

    fun parse(root: JsonNode): String {
        if (!root.isObject)
            throw OdrlException("Expected RDF type to be a JSON object")

        return getStringOrThrow(root, "@id")
    }
}