package de.fraunhofer.aisec.glue.jsonld

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode

// TODO: Somehow generalize this? Or are different types better for this?
// TODO: Also try to resolve IDs (jackson has some functionality for this)

/**
 * Allow deserializing strings that are wrapped in an object or singleton array as is the case in expanded Json-LD
 * representation.
 * This representation can carry over to the compact representation as well in case the property has a `@type` or
 * `@language` attribute.
 */
class BoxedStringDeserializer : JsonDeserializer<String>() {
    override fun deserialize(
        p: JsonParser,
        dctx: DeserializationContext,
    ): String {
        val node = p.codec.readTree<JsonNode>(p)
        return deserializeTree(node)
    }

    private fun deserializeTree(node: JsonNode): String {
        return when {
            node.isObject -> deserializeObject(node)
            node.isArray -> deserializeArray(node)
            node.isTextual -> node.asText()
            // TODO: What exception type?, Handle nullability
            else -> throw RuntimeException("node")
        }
    }

    private fun deserializeArray(node: JsonNode): String {
        if (node.size() != 1) {
            // TODO: Should we relax that a bit?
            throw RuntimeException("Array must be of length 1 to be deserialized to a string")
        }
        return deserializeTree(node[0])
    }

    private fun deserializeObject(node: JsonNode): String {
        if (node.has("@value")) {
            return deserializeTree(node["@value"])
        }

        throw RuntimeException("Cannot deserialize this object to a string")
    }
}
