package de.fraunhofer.aisec.glue.negotiation.edc

import com.apicatalog.jsonld.JsonLd
import com.apicatalog.jsonld.JsonLdOptions
import com.apicatalog.jsonld.document.JsonDocument
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import java.io.ByteArrayInputStream

object EdcQuirks {
    /**
     * While the EDC only seems to accept compact serialization of ODRL policies, it needs the shared action parameter
     * to be pushed down into the individual permissions.
     * This function moves the action parameter into each permission object, which is rather inelegant...
     * This should probably be handled in the serializer instead.
     */
    fun pushDownAction(compactSerialization: ObjectNode) {
        val actionKey = "odrl:action"
        val permissionKey = "permission"

        if (!compactSerialization.has(actionKey)) {
            // Already done or not serialized in compact form
            return
        }

        val action = compactSerialization.remove(actionKey)

        when (val permission = compactSerialization.get(permissionKey)) {
            is ArrayNode ->
                permission.forEach {
                    (it as? ObjectNode)?.putIfAbsent(
                        actionKey,
                        action,
                    ) ?: throw AssertionError("Unexpected type ${it.javaClass.simpleName} for permission")
                }
            is ObjectNode -> permission.putIfAbsent(actionKey, action)
            else -> throw AssertionError("Unexpected type ${permission.javaClass.simpleName} for permission")
        }
    }

    /**
     * The ODRL-Parser encodes the id of an action with refinements as an RDF value.
     * The EDC's parser does not understand this and complains that the action has no @id field.
     * This function unwraps each rdf:value that is found directly beneath an odrl:action object node.
     */
    fun unwrapRdfValue(serialization: ObjectNode) {
        val actionKey = "odrl:action"
        val valueKey = "rdf:value"
        val uidKey = "uid"

        serialization.findValues(actionKey).filter { it is ObjectNode && it.has(valueKey) }.forEach {
            val node = it as ObjectNode
            val rdfValue = node.remove(valueKey) as? ObjectNode ?: return@forEach
            node.set<JsonNode>("@id", rdfValue.get(uidKey))
        }
    }

    /**
     * For some reason, the jsonld compaction makes `@type` to `type` in the [EdcNegotiationManager]'s call to
     * [ObjectMapper.writeValueAsString].
     * While this gets somehow remedied before we send out our requests it is an issue when we stringify values for the
     * EDC.
     * For this reason, this function recursively replaces all `type` properties with `@type`.
     */
    fun rewriteTypeFields(node: JsonNode) {
        when (node) {
            is ObjectNode -> {
                if (node.has("type")) {
                    val type = node.remove("type")
                    val copy = mutableListOf<Map.Entry<String, JsonNode>>()
                    node.fields().forEach { copy.add(it) }
                    node.removeAll()
                    node.set<JsonNode>("@type", type)
                    copy.forEach { (k, v) -> node.set<JsonNode>(k, v) }
                }

                node.elements().forEach(::rewriteTypeFields)
            }
            is ArrayNode -> {
                node.elements().forEach(::rewriteTypeFields)
            }
        }
    }

    /**
     * As all the GLUE-tags in ODRL policies are not in the actual standard, the EDC is unable to work with them.
     * Instead of just ignoring them or passing them through as strings, the EDC just stringifies all of our items, but
     * only during serialization.
     * This has the awkward effect of messing up internal comparisons (e.g. when comparing offer and agreement) in the
     * EDC, as the agreement is stringified and the offer is a normal json tree.
     * This function tries to prevent this by preemptively converting all nodes of selected glue types to string.
     */
    fun stringifyGlueKeys(objectMapper: ObjectMapper, serialization: ObjectNode) {
        val glueTypes = listOf("http://fhg.de/glue/ContainerPipeline")

        // This function returns true if the currently processed node should be replaced.
        fun shouldStringify(node: JsonNode): Boolean {
            return when {
                node is ObjectNode -> {
                    // This hard to parse comparison means "node has a type property and its value is in glueTypes"
                    node.get("@type")?.let { type -> glueTypes.any { type == TextNode(it) } } == true
                }
                else -> false
            }
        }

        // Of course the EDC stores all values in expanded form, so we need to convert to compact JsonLd and then to
        fun stringifyNode(node: JsonNode): String {
            val compactString = objectMapper.writeValueAsString(node)
            val document = JsonDocument.of(ByteArrayInputStream(compactString.toByteArray()))
            val expanded = JsonLd.expand(document).options(JsonLdOptions()).get()
            return expanded[0].toString()
        }

        // Recursively checks if nodes should be stringified
        fun stringifyTree(node: JsonNode) {
            when (node) {
                is ObjectNode -> {
                    val stringifiedNodes = mutableMapOf<String, String>()
                    for ((key, value) in node.fields()) {
                        if (shouldStringify(value)) {
                            // We do replacements after as I'm not sure if replacing inline invalidates the iterator
                            stringifiedNodes[key] = stringifyNode(value)
                        } else {
                            stringifyTree(value)
                        }
                    }

                    for ((key, value) in stringifiedNodes) {
                        node.put(key, value)
                    }
                }

                is ArrayNode -> {
                    val stringifiedNodes = mutableMapOf<Int, String>()
                    for ((index, value) in node.elements().withIndex()) {
                        if (shouldStringify(value)) {
                            stringifiedNodes[index] = stringifyNode(value)
                        } else {
                            stringifyTree(value)
                        }
                    }

                    for ((index, value) in stringifiedNodes) {
                        node[index] = value
                    }
                }
            }
        }

        stringifyTree(serialization)
    }
}
