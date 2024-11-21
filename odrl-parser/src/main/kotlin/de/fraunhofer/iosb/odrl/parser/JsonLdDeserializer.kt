package de.fraunhofer.iosb.odrl.parser

import com.apicatalog.jsonld.JsonLd
import com.apicatalog.jsonld.JsonLdOptions
import com.apicatalog.jsonld.document.JsonDocument
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import jakarta.json.JsonValue
import java.io.StringReader

class JsonLdDeserializer<T>(
    private val mapper: ObjectMapper,
    private val options: JsonLdOptions,
    private val clazz: Class<T>
) : JsonDeserializer<T>() {

    override fun deserialize(
        jp: JsonParser,
        ctx: DeserializationContext
    ): T {
        val jsonDocument = JsonDocument.of(StringReader(ctx.readTree(jp).toString()))
        val jsonLdObject = JsonLd.compact(jsonDocument, JsonDocument.of(JsonValue.EMPTY_JSON_OBJECT))
            .options(options).compactArrays(true).compactToRelative(false).get()
        val bean = mapper.readValue(jsonLdObject.toString(), clazz)
        if (bean is JsonLdBeanValidator) {
            val validationErrors = bean.validate()
            if (validationErrors.isNotEmpty())
                throw JsonMappingException(jp, validationErrors.toString())
        }
        return bean
    }

    override fun deserializeWithType(
        jp: JsonParser,
        ctx: DeserializationContext,
        typeDeserializer: TypeDeserializer?,
    ): T {
        return deserialize(jp, ctx)
    }

}