package de.fraunhofer.iosb.odrl.parser

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.BeanProperty
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.deser.ContextualDeserializer
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import com.fasterxml.jackson.databind.node.ObjectNode

class OdrlPolicyDeserializer<T>(
    private val defaultDeserializer: JsonDeserializer<*>,
    private val clazz: Class<T>,
) : JsonDeserializer<T>(), ResolvableDeserializer, ContextualDeserializer {

    private val processor = OdrlRuleCompositionProcessor()

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
        if (root is ObjectNode)
            processor.atomizeOdrlPolicy(root)
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

}