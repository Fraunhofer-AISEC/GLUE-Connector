package de.fraunhofer.aisec.glue.negotiation.edc

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode

object EdcQuirks {
    /**
     * While the EDC only seems to accept compact serialization of ODRL policies, it needs the shared action parameter
     * to be pushed down into the individual permissions.
     * This function moves the action parameter into each permission object, which is rather inelegant...
     * This should probably be handled in the serializer instead.
     */
    fun pushDownAction(compactSerialization: ObjectNode) {
        // TODO: This really needs to  be done in the parser, so the deterministic OdrlIdentifiers can be used...
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
}
