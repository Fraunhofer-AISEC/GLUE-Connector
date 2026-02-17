package de.fraunhofer.aisec.glue.negotiation.edc

import com.apicatalog.jsonld.JsonLd
import com.apicatalog.jsonld.JsonLdOptions
import com.apicatalog.jsonld.document.JsonDocument
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import de.fraunhofer.aisec.glue.models.negotiation.Namespaces
import de.fraunhofer.iosb.odrl.OdrlIdentifiers
import de.fraunhofer.iosb.odrl.OdrlPolicy
import de.fraunhofer.iosb.odrl.glue.GlueIdentifiers
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.ByteArrayInputStream
import java.net.URI
import java.net.URISyntaxException
import kotlin.reflect.full.findAnnotation

// Prefix to make base64-"URI"s of EDC RFC-compliant
val EDC_URI_COMPAT_SCHEME = "x-edc"

object EdcQuirks {
    // Compact JSON-LD identifiers
    const val ACTION_KEY = "odrl:action"
    const val LEFT_OPERAND_KEY = "odrl:leftOperand"
    const val RIGHT_OPERAND_KEY = "odrl:rightOperand"

    // Expanded JSON-LD identifiers
    const val ACTION_KEY_EXPANDED = OdrlIdentifiers.HAS_ACTION

    // Compact JSON-LD `@type` fields that may need URI fixing
    val URI_FIX_COMPACT_TYPES by lazy {
        val types = listOf(OdrlPolicy::class) + OdrlPolicy::class.findAnnotation<JsonSubTypes>()!!.value.map { it.value }
        types.map { it.findAnnotation<JsonTypeName>()!!.value.replace("${Namespaces.ODRL}/", "odrl:") }
    }

    val GLUE_TYPES = listOf(GlueIdentifiers.CONTAINER_PIPELINE)

    val LOG = KotlinLogging.logger {}

    fun JsonNode.walkObjects(action: ObjectNode.() -> Unit) {
        when (this) {
            is ObjectNode -> {
                action(this)
                this.forEach { node ->
                    node.walkObjects(action)
                }
            }
            is ArrayNode -> forEach { it.walkObjects(action) }
        }
    }

    /**
     * While the EDC only seems to accept compact serialization of ODRL policies, it needs the shared action parameter
     * to be pushed down into the individual permissions.
     * This function moves the action parameter into each permission object, which is rather inelegant...
     * This should probably be handled in the serializer instead.
     */
    fun pushDownAction(compactSerialization: ObjectNode) {
        val permissionKey = "permission"

        if (!compactSerialization.has(ACTION_KEY)) {
            // Already done or not serialized in compact form
            return
        }

        val action = compactSerialization.remove(ACTION_KEY)

        when (val permission = compactSerialization.get(permissionKey)) {
            is ArrayNode ->
                permission.forEach {
                    (it as? ObjectNode)?.putIfAbsent(
                        ACTION_KEY,
                        action,
                    ) ?: throw AssertionError("Unexpected type ${it.javaClass.simpleName} for permission")
                }
            is ObjectNode -> permission.putIfAbsent(ACTION_KEY, action)
            else -> throw AssertionError("Unexpected type ${permission.javaClass.simpleName} for permission")
        }
    }

    /**
     * The ODRL-Parser encodes the id of an action with refinements as an RDF value.
     * The EDC's parser does not understand this and complains that the action has no @id field.
     * This function unwraps each rdf:value found directly beneath an odrl:action object node.
     */
    fun unwrapActionRdfValue(serialization: ObjectNode) {
        val valueKey = "rdf:value"
        val uidKey = "uid"

        serialization.findValues(ACTION_KEY).filter { it is ObjectNode && it.has(valueKey) }.forEach {
            val node = it as ObjectNode
            val rdfValue = node.remove(valueKey) as? ObjectNode ?: return@forEach
            node.set<JsonNode>("@id", rdfValue.get(uidKey))
        }
    }

    /**
     * For some reason, the jsonld compaction makes `@type` to `type` in the [EdcSidecarNegotiationManager]'s call to
     * [ObjectMapper.writeValueAsString].
     * While this gets somehow remedied before we send out our requests it is an issue when we stringify values for the
     * EDC.
     * For this reason, this function recursively replaces all `type` properties with `@type`.
     */
    @Suppress("DEPRECATION")
    @Deprecated("Should have been fixed upstream")
    fun rewriteTypeFields(node: JsonNode) {
        node.walkObjects {
            remove("type").let { type ->
                val copy = mutableListOf<Map.Entry<String, JsonNode>>()
                properties().forEach { copy.add(it) }
                removeAll()
                set<JsonNode>("@type", type)
                copy.forEach { (k, v) -> set<JsonNode>(k, v) }
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
    fun stringifyGlueKeys(
        objectMapper: ObjectMapper,
        serialization: ObjectNode,
    ) {
        // This function returns true if the currently processed node should be replaced.
        fun shouldStringify(node: JsonNode): Boolean {
            return when {
                node is ObjectNode -> {
                    // This hard to parse comparison means "node has a type property and its value is in glueTypes"
                    node.get("@type")?.let { type -> GLUE_TYPES.any { type == TextNode(it) } } == true
                }
                else -> false
            }
        }

        // The EDC stores its stringified values in expanded representation,
        // so we need to convert to compact JsonLd and back to expanded representation.
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
                    for ((key, value) in node.properties()) {
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

    /**
     * De-stringify nodes that the EDC does not know and therefore converts to string.
     * To keep this simple, we only process objects with known "leftOperand" properties and try to fail gracefully
     */
    fun destringifyOperators(
        objectMapper: ObjectMapper,
        jsonNode: JsonNode,
    ) {
        fun shouldDestringify(objectNode: ObjectNode): Boolean {
            if (!(objectNode.has(LEFT_OPERAND_KEY) && objectNode.has(RIGHT_OPERAND_KEY))) {
                return false
            }

            val leftOp = objectNode[LEFT_OPERAND_KEY]
            val rightOp = objectNode[RIGHT_OPERAND_KEY]

            // Check if the right op is a string and if the left op is known
            return rightOp.isTextual && leftOp.isTextual && GLUE_TYPES.contains(leftOp.asText())
        }

        jsonNode.walkObjects {
            if (shouldDestringify(this)) {
                val rightOp = this[RIGHT_OPERAND_KEY].asText()
                val destringified = objectMapper.readTree(rightOp)
                set<ObjectNode>(RIGHT_OPERAND_KEY, destringified)
            }
        }
    }

    /**
     * For some inexplicable reason, the EDC wraps actions with refinements in an additional action tag.
     * The developers of the EDC apparently think that this is correct behavior, so we need to transform it into
     * something that we can use on our end.
     * [Related Issue](https://github.com/eclipse-edc/Connector/issues/4838)
     */
    fun fixDoubleAction(serialization: JsonNode) {
        serialization.findValues(ACTION_KEY_EXPANDED).filter { it is ObjectNode && it.has(ACTION_KEY_EXPANDED) }.forEach {
            val node = it as ObjectNode
            // If the child node is not an object, then something is wrong here (we could throw to let the user know)
            val childNode = it[ACTION_KEY_EXPANDED] as? ObjectNode ?: return@forEach
            // Another sanity check to catch extra content that should not be there
            if (childNode.size() != 1 || !childNode.has("@id")) {
                return@forEach
            }

            node.remove(ACTION_KEY_EXPANDED)
            node.set<ObjectNode>("@id", childNode["@id"])
        }
    }

    fun fixUriFormat(node: JsonNode): Boolean {
        var fixed = false
        node.walkObjects {
            get("@type")?.let { type ->
                if (type.textValue() in URI_FIX_COMPACT_TYPES) {
                    val uid = get("@id").textValue()
                    try {
                        URI.create(uid)
                    } catch (e: IllegalArgumentException) {
                        if (e.cause is URISyntaxException) {
                            try {
                                val newUid = uriFromEdcFormat(uid)
                                set<JsonNode>("@id", TextNode(newUid.toString()))
                                LOG.atTrace {
                                    message = "Fixed URI format"
                                    payload = mapOf("oldUri" to uid, "newUri" to newUid)
                                }
                                fixed = true
                            } catch (e: IllegalArgumentException) {
                                throw IllegalArgumentException("Failed to create valid URI using compatibility prefix: $uid", e.cause)
                            }
                        } else {
                            throw e
                        }
                    }
                }
            }
        }
        return fixed
    }

    fun revertUriFormat(node: JsonNode): Boolean {
        var fixed = false
        node.walkObjects {
            get("@type")?.let { type ->
                if (type.textValue() in URI_FIX_COMPACT_TYPES) {
                    val uid = URI.create(get("@id").textValue())
                    if (EDC_URI_COMPAT_SCHEME == uid.scheme) {
                        val newUid = uid.toEdcFormat()
                        set<JsonNode>("@id", TextNode(newUid))
                        LOG.atTrace {
                            message = "Reverted fixed URI format"
                            payload = mapOf("oldUri" to uid, "newUri" to newUid)
                        }
                        fixed = true
                    }
                }
            }
        }
        return fixed
    }
}

fun URI.toEdcFormat(): String = toString().substring(EDC_URI_COMPAT_SCHEME.length + 1)

fun uriFromEdcFormat(edcUri: String): URI = URI.create("$EDC_URI_COMPAT_SCHEME:$edcUri")
