package fhg.glue.odrl.de.fraunhofer.iosb.odrl.glue

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonTypeInfo
import fhg.glue.odrl.de.fraunhofer.iosb.odrl.parser.JsonLd
import jakarta.json.Json
import jakarta.json.JsonValue

@JsonLd(GlueIdentifiers.CONTEXT)
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
data class GlueValue(val jsonValue: JsonValue) {
    companion object {
        @JsonCreator @JvmStatic fun create(value: Boolean): GlueValue {
            return GlueValue(if (value) JsonValue.TRUE else JsonValue.FALSE)
        }
        @JsonCreator @JvmStatic fun create(value: Int): GlueValue {
            return GlueValue(Json.createValue(value))
        }
        @JsonCreator @JvmStatic fun create(value: Double): GlueValue {
            return GlueValue(Json.createValue(value))
        }
        @JsonCreator @JvmStatic fun create(value: String): GlueValue {
            return GlueValue(Json.createValue(value))
        }
        @JsonCreator @JvmStatic fun create(map: Map<String, Any?>): GlueValue {
            return GlueValue(Json.createObjectBuilder(map).build())
        }
    }
}