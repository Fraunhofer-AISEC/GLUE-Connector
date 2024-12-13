package de.fraunhofer.iosb.odrl.glue

import com.fasterxml.jackson.databind.JsonMappingException
import de.fraunhofer.iosb.odrl.de.fraunhofer.iosb.odrl.glue.GlueParameterPlaceholderCallback
import de.fraunhofer.iosb.odrl.parser.OdrlParser
import jakarta.json.Json
import jakarta.json.JsonArray
import jakarta.json.JsonObject
import jakarta.json.JsonString
import jakarta.json.JsonValue
import java.net.URI

class GlueValueProcessor {
    suspend fun resolvePlaceholders(
        glueValue: GlueValue,
        callback: GlueParameterPlaceholderCallback,
    ): GlueValue {
        return GlueValue(resolvePlaceholders(glueValue.jsonValue, callback))
    }

    private suspend fun resolvePlaceholders(
        jsonValue: JsonValue,
        callback: GlueParameterPlaceholderCallback,
    ): JsonValue {
        val parameterPlaceholder = tryConversionToPlaceholder(jsonValue)
        if (parameterPlaceholder != null)
            return callback.getPlaceholderValue(parameterPlaceholder.key)

        return when (jsonValue) {
            is JsonArray -> {
                Json.createArrayBuilder(
                    jsonValue.map { resolvePlaceholders(it, callback) }
                ).build()
            }
            is JsonObject -> {
                Json.createObjectBuilder(
                    jsonValue.mapValues { resolvePlaceholders(it.value, callback) }
                ).build()
            }
            else -> jsonValue
        }
    }

    private fun tryConversionToPlaceholder(
        jsonValue: JsonValue,
    ): GlueParameterPlaceholder? {
        if (jsonValue is JsonString) {
            try {
                if (jsonValue.string.startsWith(GlueIdentifiers.PARAMETER_PLACEHOLDER)) {
                    val uri = URI.create(jsonValue.string)
                    if (uri.query != null && uri.query.startsWith("key="))
                        return GlueParameterPlaceholder(uri.query.substringAfter("key="))
                }
            } catch (_: IllegalArgumentException) {
                // nothing
            }
        } else if (jsonValue is JsonObject) {
            try {
                val type = jsonValue["@type"]
                if (type is JsonString && type.string == GlueIdentifiers.PARAMETER_PLACEHOLDER)
                    return OdrlParser().mapper.readValue(jsonValue.toString(), GlueParameterPlaceholder::class.java)
            } catch (e: JsonMappingException) {
                println(e)
                // nothing
            }
        }
        return null
    }
}
