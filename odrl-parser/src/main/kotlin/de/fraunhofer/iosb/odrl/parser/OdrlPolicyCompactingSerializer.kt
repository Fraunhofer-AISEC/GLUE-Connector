/*
 * Copyright 2025 Fraunhofer AISEC and Fraunhofer IOSB
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
package de.fraunhofer.iosb.odrl.parser

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import com.fasterxml.jackson.databind.node.ObjectNode
import java.io.StringWriter

class OdrlPolicyCompactingSerializer<T>(
    defaultSerializer: JsonSerializer<*>,
    clazz: Class<T>,
) : JsonSerializer<T>() {
    private val processor = OdrlRuleCompositionProcessor()
    private val defaultSerializer: JsonSerializer<T>

    init {
        if (!clazz.isAssignableFrom(defaultSerializer.handledType())) {
            throw ClassCastException()
        }
        @Suppress("UNCHECKED_CAST")
        this.defaultSerializer = defaultSerializer as JsonSerializer<T>
    }

    override fun serialize(
        policy: T,
        gen: JsonGenerator,
        serializers: SerializerProvider,
    ) {
        serializeToCompactedPolicy(policy, gen, serializers, null)
    }

    override fun serializeWithType(
        policy: T,
        gen: JsonGenerator,
        serializers: SerializerProvider,
        typeSer: TypeSerializer?,
    ) {
        serializeToCompactedPolicy(policy, gen, serializers, typeSer)
    }

    private fun serializeToCompactedPolicy(
        policy: T,
        gen: JsonGenerator,
        serializers: SerializerProvider,
        typeSer: TypeSerializer?,
    ) {
        val writer = StringWriter()
        val myGen = gen.codec.factory.createGenerator(writer)
        if (typeSer != null) {
            defaultSerializer.serializeWithType(policy, myGen, serializers, typeSer)
        } else {
            defaultSerializer.serialize(policy, myGen, serializers)
        }
        myGen.flush()

        val jp = gen.codec.factory.createParser(writer.toString())
        val root = gen.codec.readTree<JsonNode>(jp)
        if (root is ObjectNode) {
            processor.compactOdrlPolicy(root)
        }
        serializers.defaultSerializeValue(root, gen)
    }
}
