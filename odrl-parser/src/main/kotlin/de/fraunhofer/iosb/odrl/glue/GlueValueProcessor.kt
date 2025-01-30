package de.fraunhofer.iosb.odrl.glue

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.POJONode
import com.fasterxml.jackson.databind.node.TextNode
import de.fraunhofer.iosb.odrl.de.fraunhofer.iosb.odrl.glue.GlueParameterPlaceholderCallback
import de.fraunhofer.iosb.odrl.parser.OdrlParser
import java.net.URI

class GlueValueProcessor(
    private val mapper: ObjectMapper = OdrlParser().mapper
) {
    suspend fun resolvePlaceholders(
        glueValue: GlueValue,
        callback: GlueParameterPlaceholderCallback,
    ): GlueValue {
        return GlueValue(resolvePlaceholders(glueValue.json, callback))
    }

    private suspend fun resolvePlaceholders(
        jsonNode: JsonNode,
        callback: GlueParameterPlaceholderCallback,
    ): JsonNode {
        val parameterPlaceholder = tryConversionToPlaceholder(jsonNode)
        if (parameterPlaceholder != null)
            return callback.getPlaceholderValue(parameterPlaceholder.key)

        return when (jsonNode) {
            is ArrayNode -> {
                ArrayNode(
                    JsonNodeFactory.instance,
                    jsonNode.map { resolvePlaceholders(it, callback) }
                )
            }
            is ObjectNode -> {
                ObjectNode(
                    JsonNodeFactory.instance,
                    jsonNode.properties().associate { Pair(it.key, resolvePlaceholders(it.value, callback)) }
                )
            }
            else -> jsonNode
        }
    }

    private fun tryConversionToPlaceholder(
        jsonNode: JsonNode,
    ): GlueParameterPlaceholder? {
        try {
            if (jsonNode is ObjectNode || jsonNode is POJONode) {
                return mapper.convertValue(jsonNode, GlueParameterPlaceholder::class.java)
            } else if (jsonNode is TextNode && jsonNode.textValue().startsWith(GlueIdentifiers.PARAMETER_PLACEHOLDER)) {
                val uri = URI.create(jsonNode.textValue())
                if (uri.query != null && uri.query.startsWith("key="))
                    return GlueParameterPlaceholder(uri.query.substringAfter("key="))
            }
        } catch (_: IllegalArgumentException) {
            // nothing
        }
        return null
    }
}
