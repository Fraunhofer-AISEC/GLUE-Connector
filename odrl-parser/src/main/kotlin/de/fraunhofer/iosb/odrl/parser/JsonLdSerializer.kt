package de.fraunhofer.iosb.odrl.parser

import com.apicatalog.jsonld.JsonLd
import com.apicatalog.jsonld.JsonLdOptions
import com.apicatalog.jsonld.document.JsonDocument
import com.apicatalog.jsonld.json.JsonUtils
import com.apicatalog.jsonld.loader.DocumentLoader
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import jakarta.json.JsonValue
import java.io.StringReader

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
        val jsonDocument = JsonDocument.of(StringReader(mapper.writeValueAsString(value)))
        val context = clazz.getAnnotation(de.fraunhofer.iosb.odrl.parser.JsonLd::class.java).context
        val jsonLdObject = JsonLd.compact(jsonDocument, context).options(options)
            .loader(documentLoader).compactToRelative(false).get()

        val jsonLdObjectMap: MutableMap<String, JsonValue> = LinkedHashMap(jsonLdObject)
        jsonLdObjectMap.replace("@context", JsonUtils.toJsonValue(context))

        gen.writeRawValue(JsonUtils.toJsonObject(jsonLdObjectMap).toString())
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