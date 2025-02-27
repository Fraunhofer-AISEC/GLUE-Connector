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
        serializers: SerializerProvider
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
            if (action.includedIn != null)
                serializers.defaultSerializeField(OdrlIdentifiers.INCLUDED_IN, action.includedIn, gen)
            if (action.implies.isNotEmpty())
                serializers.defaultSerializeField(OdrlIdentifiers.IMPLIES, action.implies, gen)
            gen.writeEndObject()
        }
    }

    override fun serializeWithType(
        action: OdrlAction,
        gen: JsonGenerator,
        serializers: SerializerProvider,
        typeSer: TypeSerializer?
    ) {
        serialize(action, gen, serializers)
    }

}