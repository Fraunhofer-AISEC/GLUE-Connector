package de.fraunhofer.iosb.odrl.parser

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import de.fraunhofer.iosb.odrl.OdrlIdentifiers

class OdrlRuleCompositionProcessor {
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

    fun atomizeOdrlPolicy(policy: ObjectNode) {
        moveSharedPropertiesToRules(policy)
        atomizeRules(policy)
    }

    private fun moveSharedPropertiesToRules(policy: ObjectNode) {
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

    private fun addPropertyToNode(
        node: JsonNode,
        key: String,
        value: JsonNode
    ) {
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

    private fun atomizeRules(policy: ObjectNode) {
        for (ruleKey in ruleKeys) {
            val rule = policy.get(ruleKey) ?: continue
            var ruleList = if (rule is ArrayNode) rule.toList() else listOf(rule)
            for (propKey in sharedPropKeys)
                ruleList = ruleList.flatMap { reduceOdrlRuleForKey(it, propKey) }
            policy.replace(ruleKey, ArrayNode(JsonNodeFactory.instance, ruleList))
        }
    }

    private fun reduceOdrlRuleForKey(
        rule: JsonNode,
        key: String
    ): List<JsonNode> {
        if (rule !is ObjectNode)
            return listOf()
        val prop = rule.get(key)
        if (prop !is ArrayNode)
            return listOf(rule)
        return prop.map { rule.deepCopy().set(key, it) }
    }

    fun compactOdrlPolicy(policy: ObjectNode) {
        consolidateRules(policy)
        moveSharedPropertiesToPolicy(policy)
    }

    private fun consolidateRules(policy: ObjectNode) {
        for (ruleKey in ruleKeys) {
            val rules = policy.get(ruleKey)
            if (rules !is ArrayNode)
                continue
            do {
                val found = consolidateNextRules(rules)
            } while (found)
        }
    }

    private fun consolidateNextRules(rules: ArrayNode): Boolean {
        for (keyToConsolidate in sharedPropKeys) {
            for (r in rules) {
                if (r !is ObjectNode || !r.hasNonNull(keyToConsolidate))
                    continue
                // Rules to consolidate with r must be object nodes...
                val rulesToConsolidate = rules.filterIsInstance<ObjectNode>()
                    // ...have exactly the same fields as r...
                    .filter { r.fieldNames().asSequence().toSet() == it.fieldNames().asSequence().toSet() }
                    // ...and must agree on all property values except at the field to consolidate
                    .filter { r2 ->
                        r.fieldNames().asSequence().minus(keyToConsolidate).all { r.get(it).equals(r2.get(it)) }
                    }
                if (rulesToConsolidate.size > 1) {
                    val consolidatedValues = rulesToConsolidate.map { it.get(keyToConsolidate) }.distinct()
                    r.replace(keyToConsolidate, ArrayNode(JsonNodeFactory.instance, consolidatedValues))
                    rules.removeAll { rulesToConsolidate.minusElement(r).contains(it) }
                    return true
                }
            }
        }
        return false
    }

    private fun moveSharedPropertiesToPolicy(policy: ObjectNode) {
        val rules = ruleKeys.filter { policy.hasNonNull(it) }.map { policy.get(it) }
            .flatMap { if (it is ArrayNode) it.elements().asSequence().toList() else listOf(it) }
        if (!rules.all { it is ObjectNode })
            return
        for (propKey in sharedPropKeys) {
            val propertyValues = rules.map { it.get(propKey) }
            if (propertyValues.isNotEmpty() && propertyValues.all { it != null && it.equals(propertyValues.first()) }) {
                policy.replace(propKey, propertyValues.first())
                rules.forEach { (it as ObjectNode).remove(propKey) }
            }
        }
    }
}