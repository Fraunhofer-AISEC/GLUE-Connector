/*
 * Copyright 2024 Fraunhofer AISEC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.fraunhofer.aisec.glue.jsonld

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode

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

    private fun deserializeTree(node: JsonNode): String =
        when {
            node.isObject -> deserializeObject(node)
            node.isArray -> deserializeArray(node)
            node.isTextual -> node.asText()
            else -> throw RuntimeException("node")
        }

    private fun deserializeArray(node: JsonNode): String {
        if (node.size() != 1) {
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
