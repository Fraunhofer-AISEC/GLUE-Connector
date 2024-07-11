package de.fraunhofer.aisec.glue.jsonld

import com.apicatalog.jsonld.JsonLd
import com.apicatalog.jsonld.JsonLdOptions
import com.apicatalog.jsonld.document.JsonDocument
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import jakarta.json.JsonObject

class JsonLDSerializer<T : Any>(private val options: JsonLdOptions, private val mapper: ObjectMapper, cls: Class<T>) : JsonSerializer<T>() {
    override fun serialize(
        obj: T,
        gen: JsonGenerator,
        sp: SerializerProvider,
    ) {
        val ctx = "https://w3id.org/dspace/2024/1/context.json"
        val jsonObject = mapper.convertValue(obj, JsonObject::class.java)
        val transformed =
            JsonLd.compact(JsonDocument.of(jsonObject), ctx)
                .options(options)
                .get()

        mapper.writeValue(gen, transformed)
    }

    override fun serializeWithType(
        value: T,
        gen: JsonGenerator,
        serializers: SerializerProvider,
        typeSer: TypeSerializer?,
    ) {
        // The type serialization is taken care of by the object mapper in serialize
        serialize(value, gen, serializers)
    }
}
