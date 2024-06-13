package fhg.glue.odrl

import com.fasterxml.jackson.databind.JsonNode

class OdrlAssetParser: OdrlParser() {
    override val key: String = "asset"

    fun parse(root: JsonNode): OdrlAsset {
        if (!root.isTextual)
            throw OdrlException("Expected action to be a string")

        // An Asset SHOULD have one uid property value of type IRI
        val uid = root.asText()
        isIri(uid)


        return OdrlAsset(uid)
    }
}