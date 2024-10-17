package de.fraunhofer.aisec.glue.jsonld

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper

/**
 * Indicates, that a value of the type T should use the jsonld processor
 */
@JsonLDType
data class AsJsonLd<T> @JsonCreator constructor(@JsonValue val value: T) {
    companion object {
        inline fun <reified T> ObjectMapper.convertFromJsonLd(value: Any): T {
            return convertValue(value, object : TypeReference<AsJsonLd<T>>() {}).value
        }
    }
}