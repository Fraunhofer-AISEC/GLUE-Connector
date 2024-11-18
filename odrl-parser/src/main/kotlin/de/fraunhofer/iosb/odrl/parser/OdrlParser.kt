package fhg.glue.odrl.de.fraunhofer.iosb.odrl.parser

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.util.StdDateFormat
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import fhg.glue.odrl.de.fraunhofer.iosb.odrl.OdrlPolicy
import java.io.Reader

class OdrlParser(val mapper: ObjectMapper = ObjectMapper()) {
    init {
        configureMapper()
        mapper.registerModules(JsonLdModule(mapper.copy()))
    }

    private fun configureMapper() {
        mapper
            .registerKotlinModule()
            .registerModule(JavaTimeModule())
            .setDateFormat(StdDateFormat().withColonInTimeZone(true)) // Provides ISO time support
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)    // Hides empty properties in serialized policies
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY) // Accepts single properties as list
    }

    fun readOdrlPolicy(r: Reader): OdrlPolicy {
        return mapper.readValue(r, OdrlPolicy::class.java);
    }

    fun writeOdrlPolicy(policy: OdrlPolicy): String {
        return mapper.writeValueAsString(policy)
    }
}