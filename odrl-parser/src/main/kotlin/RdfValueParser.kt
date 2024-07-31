package fhg.glue.odrl

import com.fasterxml.jackson.databind.JsonNode

class RdfValueParser : OdrlParser() {
    override val key: String = "rdf-value"

    fun parse(root: JsonNode): String {
        if (root.isObject && root.has("@id")) {
            val id = root.get("@id")
            if (!id.isTextual)
                throw OdrlException("Expected property '@id' of RDF value to be a string!")
            return id.asText()
        }

        if (!root.isTextual)
            throw OdrlException("Expected RDF value to be a string or object!")

        return root.asText()
    }
}