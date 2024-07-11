package de.fraunhofer.aisec.glue.jsonld

import com.apicatalog.jsonld.JsonLd
import com.apicatalog.jsonld.JsonLdOptions
import com.apicatalog.jsonld.document.JsonDocument
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import jakarta.json.JsonObject

class JsonLDDeserializer<T : Any>(
    private val options: JsonLdOptions,
    private val mapper: ObjectMapper,
    private val cls: Class<T>,
) : JsonDeserializer<T>() {
    override fun deserialize(
        parser: JsonParser,
        ctx: DeserializationContext,
    ): T {
        val jsonObject = mapper.readValue(parser, JsonObject::class.java)
        val transformed =
            JsonLd.compact(JsonDocument.of(jsonObject), JsonDocument.of(JsonObject.EMPTY_JSON_OBJECT))
                .options(options)
                .get()

        return mapper.convertValue(transformed, cls)
    }

    override fun deserializeWithType(
        p: JsonParser,
        ctxt: DeserializationContext,
        typeDeserializer: TypeDeserializer?,
    ): Any {
        // The type is handled by the object mapper and must be checked after jsonld processing
        return deserialize(p, ctxt)
    }
}
