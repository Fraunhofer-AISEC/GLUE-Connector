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

        val parameters = mutableListOf<GlueParameter>()
        if (root.has(GlueOdrlIdentifiers.HAS_PARAMETER)) {
            val parameterNode = root.get(GlueOdrlIdentifiers.HAS_PARAMETER)
            if (parameterNode.isObject) {
                parameters.add(GlueParameterParser().parse(parameterNode))
            } else if (parameterNode.isArray) {
                for (p in parameterNode.elements())
                    parameters.add(GlueParameterParser().parse(p))
            } else {
                throw OdrlException("Expected field 'parameter' to be a JSON object or array")
            }
        }

        var allowNetwork = false;
        if (root.has(GlueOdrlIdentifiers.ALLOW_NETWORK)) {
            allowNetwork = root.get(GlueOdrlIdentifiers.ALLOW_NETWORK).asBoolean(false)
        }

        return GlueContainer(image, hash, parameters, allowNetwork);
    }
}