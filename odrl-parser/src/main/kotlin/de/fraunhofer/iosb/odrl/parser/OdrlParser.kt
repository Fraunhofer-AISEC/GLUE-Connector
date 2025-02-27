package de.fraunhofer.iosb.odrl.parser

import com.apicatalog.jsonld.JsonLdOptions
import com.apicatalog.jsonld.loader.DocumentLoader
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.util.StdDateFormat
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import de.fraunhofer.iosb.odrl.OdrlPolicy
import de.fraunhofer.iosb.odrl.parser.JsonLdModule.Companion.createDocumentLoader
import java.io.Reader

@Suppress("unused")
class OdrlParser(
    val mapper: ObjectMapper = ObjectMapper(),
    options: JsonLdOptions = JsonLdOptions(),
    documentLoader: DocumentLoader = createDocumentLoader(),
) {
    init {
        mapper.registerKotlinModule()
        val odrlMapper = mapper.copy()
            .registerModule(OdrlModule())
            .registerModule(JavaTimeModule())
            .setDateFormat(StdDateFormat().withColonInTimeZone(true)) // Provides ISO time support
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)    // Hides empty properties in serialized policies
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY) // Accepts single properties as list
        mapper.registerModules(JsonLdModule(odrlMapper, options, documentLoader))
    }

    fun readOdrlPolicy(r: Reader): OdrlPolicy {
        return mapper.readValue(r, OdrlPolicy::class.java)
    }

    fun convertToOdrlPolicy(value: Any): OdrlPolicy {
        return mapper.convertValue(value, OdrlPolicy::class.java)
    }

    fun writeOdrlPolicy(policy: OdrlPolicy): String {
        return mapper.writeValueAsString(policy)
    }
}