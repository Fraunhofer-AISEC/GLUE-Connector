package de.fraunhofer.iosb.odrl.parser

import com.apicatalog.jsonld.JsonLd
import com.apicatalog.jsonld.JsonLdOptions
import com.apicatalog.jsonld.document.JsonDocument
import com.apicatalog.jsonld.loader.DocumentLoader
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import de.fraunhofer.iosb.odrl.OdrlIdentifiers
import jakarta.json.JsonValue
import java.io.StringReader

class JsonLdDeserializer<T>(
    private val mapper: ObjectMapper,
    private val options: JsonLdOptions,
    private val documentLoader: DocumentLoader,
    private val clazz: Class<T>,
) : JsonDeserializer<T>() {

    override fun deserialize(
        jp: JsonParser,
        ctx: DeserializationContext
    ): T {
        val jsonDocument = JsonDocument.of(StringReader(ctx.readTree(jp).toString()))
        val jsonLdObject = JsonLd.compact(jsonDocument, JsonDocument.of(JsonValue.EMPTY_JSON_OBJECT))
            .options(options).loader(documentLoader).compactArrays(true).compactToRelative(false).get()

        val jsonLdString = jsonLdObject.toString()
            .replace("http://www.w3.org/ns/odrl/2/neg", OdrlIdentifiers.NEQ)

        val bean = mapper.readValue(jsonLdString, clazz)
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