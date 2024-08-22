package fhg.glue.odrl

import com.fasterxml.jackson.databind.JsonNode

class GlueContainerParser : OdrlParser() {
    override val key: String = "glue-container"

    fun parse(root: JsonNode): GlueContainer {
        if (!root.isObject) {
            throw OdrlException("Expected GLUE container to be an object!")
        }

        val image = getStringOrThrow(root, GlueOdrlIdentifiers.IMAGE)
        val hash = getStringOrThrow(root, GlueOdrlIdentifiers.HASH)

        var entrypoint: String? = null;
        if (root.has(GlueOdrlIdentifiers.ENTRYPOINT)) {
            entrypoint = root.get(GlueOdrlIdentifiers.ENTRYPOINT).asText(null)
        }

        var allowNetwork = false;
        if (root.has(GlueOdrlIdentifiers.ALLOW_NETWORK)) {
            allowNetwork = root.get(GlueOdrlIdentifiers.ALLOW_NETWORK).asBoolean(false)
        }

        return GlueContainer(image, hash, entrypoint, allowNetwork);
    }
}