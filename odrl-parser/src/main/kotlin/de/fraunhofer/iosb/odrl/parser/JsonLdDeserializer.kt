package de.fraunhofer.iosb.odrl.parser

import com.apicatalog.jsonld.JsonLd
import com.apicatalog.jsonld.JsonLdOptions
import com.apicatalog.jsonld.document.JsonDocument
import com.apicatalog.jsonld.json.JsonProvider
import com.apicatalog.jsonld.loader.DocumentLoader
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import de.fraunhofer.iosb.odrl.OdrlIdentifiers
import jakarta.json.JsonString
import jakarta.json.JsonStructure
import jakarta.json.JsonValue

class JsonLdDeserializer<T>(
    private val mapper: ObjectMapper,
    private val options: JsonLdOptions,
    private val documentLoader: DocumentLoader,
    private val clazz: Class<T>,
) : JsonDeserializer<T>() {

    private val jsonProvider = JsonProvider.instance()

    override fun deserialize(
        jp: JsonParser,
        ctx: DeserializationContext
    ): T {
        val jsonStructure = mapper.readValue(jp, JsonStructure::class.java)
        val jsonDocument = JsonDocument.of(jsonStructure)
        val jsonLdObject = JsonLd.compact(jsonDocument, JsonDocument.of(JsonValue.EMPTY_JSON_OBJECT))
            .options(options).loader(documentLoader).compactArrays(true).compactToRelative(false).get()

        val fixedJsonLdObject = replaceStringValues(
            jsonLdObject,
            "http://www.w3.org/ns/odrl/2/neg",
            OdrlIdentifiers.NEQ
        )

        val bean = mapper.convertValue(fixedJsonLdObject, clazz)
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

    private fun replaceStringValues(value: JsonValue, from: String, to: String): JsonValue {
        return when (value.valueType) {
            JsonValue.ValueType.OBJECT -> {
                val builder = jsonProvider.createObjectBuilder()
                value.asJsonObject().forEach { (key, entryValue) ->
                    val newKey = key.replace(from, to)
                    builder.add(newKey, replaceStringValues(entryValue, from, to))
                }
                builder.build()
            }
            JsonValue.ValueType.ARRAY -> {
                val builder = jsonProvider.createArrayBuilder()
                value.asJsonArray().forEach { entryValue ->
                    builder.add(replaceStringValues(entryValue, from, to))
                }
                builder.build()
            }
            JsonValue.ValueType.STRING -> {
                val jsonString = value as JsonString
                val replaced = jsonString.string.replace(from, to)
                if (replaced == jsonString.string) value else jsonProvider.createValue(replaced)
            }
            else -> value
        }
    }

}
