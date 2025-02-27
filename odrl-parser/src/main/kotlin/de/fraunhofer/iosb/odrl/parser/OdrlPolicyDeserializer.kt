package de.fraunhofer.iosb.odrl.parser

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.BeanProperty
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.deser.ContextualDeserializer
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import de.fraunhofer.iosb.odrl.OdrlIdentifiers

class OdrlPolicyDeserializer<T>(
    private val defaultDeserializer: JsonDeserializer<*>,
    private val clazz: Class<T>,
) : JsonDeserializer<T>(), ResolvableDeserializer, ContextualDeserializer {

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

    override fun resolve(ctx: DeserializationContext?) {
        (defaultDeserializer as ResolvableDeserializer).resolve(ctx)
    }

    override fun createContextual(
        ctx: DeserializationContext?,
        property: BeanProperty?
    ): JsonDeserializer<*> {
        return this
    }

    override fun deserialize(
        jp: JsonParser,
        ctx: DeserializationContext
    ): T {
        val root = ctx.readTree(jp)
        if (root is ObjectNode) {
            // Expand policy: Shared properties on policy-level must be moved to rules
            expandOdrlPolicy(root)
            // Reduce rules: Composite rules with extended property cardinalities must be atomized
            reduceOdrlRules(root)
        }
        val rootParser = root.traverse(jp.codec)
        rootParser.nextToken()
        return clazz.cast(defaultDeserializer.deserialize(rootParser, ctx))
    }

    override fun deserializeWithType(
        jp: JsonParser,
        ctx: DeserializationContext,
        typeDeserializer: TypeDeserializer?,
    ): T {
        return clazz.cast(defaultDeserializer.deserializeWithType(jp, ctx, typeDeserializer))
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

    private fun expandOdrlPolicy(policy: ObjectNode) {
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

    private fun reduceOdrlRules(policy: ObjectNode) {
        for (ruleKey in ruleKeys) {
            val rule = policy.get(ruleKey) ?: continue
            var ruleList = if (rule is ArrayNode) rule.toList() else listOf(rule)
            for (propKey in sharedPropKeys)
                ruleList = ruleList.flatMap { reduceOdrlRuleForKey(it, propKey) }
            policy.replace(ruleKey, ArrayNode(JsonNodeFactory.instance, ruleList))
        }
    }

}