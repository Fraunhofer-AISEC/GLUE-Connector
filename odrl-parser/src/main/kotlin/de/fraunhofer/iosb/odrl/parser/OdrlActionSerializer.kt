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
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import de.fraunhofer.iosb.odrl.OdrlAction
import de.fraunhofer.iosb.odrl.OdrlIdentifiers
import de.fraunhofer.iosb.odrl.RdfIdentifiers

class OdrlActionSerializer : JsonSerializer<OdrlAction>() {
    override fun serialize(
        action: OdrlAction,
        gen: JsonGenerator,
        serializers: SerializerProvider,
    ) {
        if (action.refinement.isEmpty() && action.includedIn == null && action.implies.isEmpty()) {
            gen.writeString(action.value.uid.toString()) // Compact format: Action consists of only id
        } else {
            gen.writeStartObject()
            if (action.refinement.isNotEmpty()) {
                // Refined format: Use rdf:value to represent action instance
                serializers.defaultSerializeField(RdfIdentifiers.VALUE, action.value, gen)
                serializers.defaultSerializeField(OdrlIdentifiers.REFINEMENT, action.refinement, gen)
            } else {
                // Complex format: Use @id to represent action instance
                gen.writeStringField("@id", action.value.uid.toString())
            }
            if (action.includedIn != null) {
                serializers.defaultSerializeField(OdrlIdentifiers.INCLUDED_IN, action.includedIn, gen)
            }
            if (action.implies.isNotEmpty()) {
                serializers.defaultSerializeField(OdrlIdentifiers.IMPLIES, action.implies, gen)
            }
            gen.writeEndObject()
        }
    }

    override fun serializeWithType(
        action: OdrlAction,
        gen: JsonGenerator,
        serializers: SerializerProvider,
        typeSer: TypeSerializer?,
    ) {
        serialize(action, gen, serializers)
    }
}
