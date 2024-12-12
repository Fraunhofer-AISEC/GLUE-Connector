package de.fraunhofer.iosb.odrl.glue

import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import de.fraunhofer.iosb.odrl.parser.JsonLd

@JsonLd(GlueIdentifiers.CONTEXT)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = GlueParameter::class)
@JsonTypeName(GlueIdentifiers.PARAMETER)
@JsonSubTypes(
    Type(GlueRestParameter::class),
)
open class GlueParameter {
    @JsonProperty(GlueIdentifiers.VALUE)
    val value: GlueValue? = null

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