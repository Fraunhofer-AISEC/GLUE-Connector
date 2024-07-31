package fhg.glue.odrl

import com.fasterxml.jackson.databind.JsonNode

class OdrlAssetParser: OdrlParser() {
    override val key: String = "asset"

    fun parse(root: JsonNode): OdrlAsset {
        if (root.isTextual) {
            isIri(root.asText())
            return OdrlAsset(root.asText())
        }

        if (!root.isObject)
            throw OdrlException("Expected asset to be a string or object")

        // An Asset SHOULD have one uid property value of type IRI
        val uid = getStringOrThrow(root, "@id")
        isIri(uid)


        return OdrlAsset(uid)
    }
}