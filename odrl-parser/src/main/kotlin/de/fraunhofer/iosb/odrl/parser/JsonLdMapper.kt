package de.fraunhofer.iosb.odrl.parser

import com.fasterxml.jackson.databind.ObjectMapper

open class JsonLdMapper<T>(
    val mapper: ObjectMapper,
    val clazz: Class<T>,
) {
    open fun readValue(jsonLd: String): T {
        return mapper.readValue(jsonLd, clazz)
    }
}