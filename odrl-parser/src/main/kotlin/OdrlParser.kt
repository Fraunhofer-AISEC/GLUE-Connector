package fhg.glue.odrl

import java.net.URI
import com.fasterxml.jackson.databind.JsonNode

abstract class OdrlParser {
    abstract val key: String

    fun isIri(iri: String): Boolean {
        try {
            URI.create(iri)
            return true
        } catch (e: IllegalArgumentException) {
            return false
        }
    }

    fun getFieldOrThrow(node: JsonNode, fieldName: String): JsonNode {
        if(!node.has(fieldName)) {
            throw OdrlException("Expected field $fieldName in ODRL $key")
        }
        return node.get(fieldName)
    }

    fun getStringOrThrow(node: JsonNode, fieldName: String): String {
        val s = getFieldOrThrow(node, fieldName)
        if(!s.isTextual) {
            throw OdrlException("Expected field $fieldName in ODRL $key to be a string")
        }
        return s.asText()
    }

    fun getIriOrThrow(node: JsonNode, fieldName: String): String {
        val s = getStringOrThrow(node, fieldName)
        if(!isIri(s)) {
            throw OdrlException("Expected field $fieldName in ODRL $key to be an IRI")
        }
        return s
    }

    fun getIntOrThrow(node: JsonNode, fieldName: String): Int {
        val i = getFieldOrThrow(node, fieldName)
        if(!i.isInt) {
            throw OdrlException("Expected field $fieldName in ODRL $key to be an integer")
        }
        return i.asInt()
    }

}