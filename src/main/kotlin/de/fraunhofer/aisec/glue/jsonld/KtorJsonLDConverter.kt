package de.fraunhofer.aisec.glue.jsonld

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import io.ktor.http.*
import io.ktor.serialization.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.util.reflect.*
import io.ktor.utils.io.*
import io.ktor.utils.io.charsets.*
import io.ktor.utils.io.jvm.javaio.*

class KtorJsonLDConverter(mapper: ObjectMapper) : ContentConverter {
    private val deserializer = JsonLDDeserializer(mapper)

    override suspend fun deserialize(charset: Charset, typeInfo: TypeInfo, content: ByteReadChannel): Any {
        return deserializer.deserialize(content.toInputStream(), typeInfo.type)
    }

    companion object {
        val defaultObjectMapper: ObjectMapper = JsonMapper.builder().addModule(kotlinModule()).build()

        fun ContentNegotiationConfig.jsonLd(mapper: ObjectMapper = defaultObjectMapper) {
            register(ContentType("application", "ld+json"), KtorJsonLDConverter(mapper))
        }
    }
}