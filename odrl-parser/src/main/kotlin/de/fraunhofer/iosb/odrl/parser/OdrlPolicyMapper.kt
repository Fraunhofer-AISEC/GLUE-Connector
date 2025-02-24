package de.fraunhofer.iosb.odrl.parser

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import de.fraunhofer.iosb.odrl.OdrlIdentifiers

class OdrlPolicyMapper<T>(
    mapper: ObjectMapper,
    clazz: Class<T>,
) : JsonLdMapper<T>(mapper, clazz) {

    private val ruleKeys = listOf(
        OdrlIdentifiers.HAS_PERMISSION,
        OdrlIdentifiers.HAS_PROHIBITION,
        OdrlIdentifiers.HAS_DUTY,
    )

    private val sharedPropKeys = listOf(
        OdrlIdentifiers.HAS_ACTION,
        OdrlIdentifiers.TARGET,
        OdrlIdentifiers.OUTPUT,
        OdrlIdentifiers.ASSIGNEE,
        OdrlIdentifiers.ASSIGNER,
        OdrlIdentifiers.ATTRIBUTED_PARTY,
        OdrlIdentifiers.ATTRIBUTING_PARTY,
        OdrlIdentifiers.COMPENSATED_PARTY,
        OdrlIdentifiers.COMPENSATING_PARTY,
        OdrlIdentifiers.CONSENTED_PARTY,
        OdrlIdentifiers.CONSENTING_PARTY,
        OdrlIdentifiers.CONTRACTED_PARTY,
        OdrlIdentifiers.CONTRACTING_PARTY,
        OdrlIdentifiers.INFORMED_PARTY,
        OdrlIdentifiers.INFORMING_PARTY,
        OdrlIdentifiers.TRACKED_PARTY,
        OdrlIdentifiers.TRACKING_PARTY,
    )

    override fun readValue(jsonLd: String): T {
        val root = mapper.readValue(jsonLd, JsonNode::class.java)

        // Expand policy: Move shared properties from policy to rules
        expandOdrlPolicy(root)

        return mapper.convertValue(root, clazz)
    }

    private fun addPropertyToNode(node: JsonNode, key: String, value: JsonNode) {
        if (node !is ObjectNode)
            return
        val existingProp = node.get(key);
        if (existingProp is ArrayNode) {
            if (value is ArrayNode)
                existingProp.addAll(value)
            else
                existingProp.add(value)
        } else {
            node.replace(key, value)
        }
    }

    private fun expandOdrlPolicy(policy: JsonNode) {
        if (policy !is ObjectNode)
            return
        for (propKey in sharedPropKeys) {
            if (!policy.hasNonNull(propKey))
                continue
            val prop = policy.remove(propKey)
            for (ruleKey in ruleKeys) {
                val rule = policy.get(ruleKey)
                if (rule is ArrayNode) {
                    rule.forEach { addPropertyToNode(it, propKey, prop) }
                } else if (rule != null) {
                    addPropertyToNode(rule, propKey, prop)
                }
            }
        }
    }

}