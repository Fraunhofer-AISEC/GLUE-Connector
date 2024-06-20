package de.fraunhofer.aisec.glue.jsonld

import com.apicatalog.jsonld.JsonLd
import com.apicatalog.jsonld.JsonLdEmbed
import com.apicatalog.jsonld.document.JsonDocument
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsonp.JSONPModule
import jakarta.json.Json
import java.io.InputStream
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

class JsonLDDeserializer(private val mapper: ObjectMapper) {
    init {
        // Needed to convert the deserialized jsonld structure to a concrete type
        mapper.registerModule(JSONPModule())
        mapper.addHandler(JsonLDProblemHandler())
    }

    fun <T: Any> deserialize(stream: InputStream, rootId: String, type: KClass<T>): T {
        val frame = Json.createObjectBuilder()
            .add("@id", rootId)
            .build()

        val deserialized = JsonLd.frame(JsonDocument.of(stream), JsonDocument.of(frame))
            .embed(JsonLdEmbed.ALWAYS)
            .omitGraph(true)
            .get()

        return mapper.convertValue(deserialized, type.java)
    }

    fun <T: Any> deserialize(stream: InputStream, type: KClass<T>): T {
        val idAnnotation = type.findAnnotation<JsonLdId>()
            ?: throw IllegalArgumentException("The id of the root object must be provided")

        return deserialize(stream, idAnnotation.id, type)
    }
}