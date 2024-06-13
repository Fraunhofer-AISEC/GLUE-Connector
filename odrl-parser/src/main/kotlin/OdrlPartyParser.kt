package fhg.glue.odrl

import com.fasterxml.jackson.databind.JsonNode

class OdrlPartyParser: OdrlParser() {
    override val key: String = "party"

    fun parse(root: JsonNode): OdrlParty {
        if (!root.isTextual)
            throw OdrlException("Expected party to be a string")

        // A Party SHOULD have one uid property value of type IRI
        val uid = root.asText()
        isIri(uid)


        return OdrlParty(uid)
    }
}