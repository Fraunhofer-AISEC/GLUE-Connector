package de.fraunhofer.iosb.odrl.parser

import com.apicatalog.jsonld.JsonLd
import com.apicatalog.jsonld.JsonLdOptions
import com.apicatalog.jsonld.document.JsonDocument
import com.apicatalog.jsonld.json.JsonUtils
import com.apicatalog.jsonld.loader.DocumentLoader
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import jakarta.json.JsonStructure
import jakarta.json.JsonValue

class JsonLdSerializer<T>(
    private val mapper: ObjectMapper,
    private val options: JsonLdOptions,
    private val documentLoader: DocumentLoader,
    private val clazz: Class<T>,
) : JsonSerializer<T>() {

    override fun serialize(
        value: T,
        gen: JsonGenerator,
        serializers: SerializerProvider
    ) {
        val jsonStructure = mapper.convertValue(value, JsonStructure::class.java)
        val jsonDocument = JsonDocument.of(jsonStructure)
        val context = clazz.getAnnotation(de.fraunhofer.iosb.odrl.parser.JsonLd::class.java).context
        val jsonLdObject = JsonLd.compact(jsonDocument, context).options(options)
            .loader(documentLoader).compactToRelative(false).get()

        val jsonLdObjectMap: MutableMap<String, JsonValue> = LinkedHashMap(jsonLdObject)
        jsonLdObjectMap.replace("@context", JsonUtils.toJsonValue(context))

        gen.writeObject(JsonUtils.toJsonObject(jsonLdObjectMap))
    }

    override fun serializeWithType(
        value: T,
        gen: JsonGenerator,
        serializers: SerializerProvider,
        typeSer: TypeSerializer?,
    ) {
        serialize(value, gen, serializers)
    }

}
