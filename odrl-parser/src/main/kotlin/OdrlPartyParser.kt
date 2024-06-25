package fhg.glue.odrl

import com.fasterxml.jackson.databind.JsonNode

class OdrlPartyParser: OdrlParser() {
    override val key: String = "party"

    fun parse(root: JsonNode): OdrlParty {
        if (root.isTextual) {
            isIri(root.asText())
            return OdrlParty(root.asText())
        }

        if (!root.isObject)
            throw OdrlException("Expected party to be a string or object")

        // A Party SHOULD have one uid property value of type IRI
        val uid = getStringOrThrow(root, "@id")
        isIri(uid)


        return OdrlParty(uid)
    }
}