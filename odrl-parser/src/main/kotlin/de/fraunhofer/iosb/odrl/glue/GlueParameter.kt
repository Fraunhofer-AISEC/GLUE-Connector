package de.fraunhofer.iosb.odrl.glue

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import de.fraunhofer.iosb.odrl.parser.JsonLd

@JsonLd(GlueIdentifiers.CONTEXT)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = GlueParameter::class)
@JsonTypeName(GlueIdentifiers.PARAMETER)
@JsonSubTypes(
    Type(GlueRestParameter::class),
    Type(GlueKeyValueParameter::class),
)
open class GlueParameter(
    // Using the @field: annotation target allows Jackson to see this property also in subclasses
    @field:JsonProperty(GlueIdentifiers.HAS_VALUE)
    val value: GlueValue? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GlueParameter

        return value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return "GlueParameter(value=$value)"
    }
}