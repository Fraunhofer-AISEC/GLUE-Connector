package de.fraunhofer.iosb.odrl.glue

import com.fasterxml.jackson.annotation.*
import de.fraunhofer.iosb.odrl.parser.JsonLd
import de.fraunhofer.iosb.odrl.parser.JsonLdBeanValidator
import io.konform.validation.Validation
import io.konform.validation.ValidationError
import io.konform.validation.string.notBlank

@JsonLd(GlueIdentifiers.CONTEXT)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = GlueContainer::class)
@JsonTypeName(GlueIdentifiers.CONTAINER)
data class GlueContainer(
    @param:JsonProperty(GlueIdentifiers.IMAGE)
    val image: String,

    @param:JsonProperty(GlueIdentifiers.HASH)
    val hash: String,

    @param:JsonProperty(GlueIdentifiers.ALLOW_NETWORK)
    val allowNetwork: Boolean? = false,

    @param:JsonProperty(GlueIdentifiers.HAS_PARAMETER)
    @param:JsonSetter(nulls = Nulls.AS_EMPTY)
    val parameter: List<GlueParameter> = listOf(),
): JsonLdBeanValidator {
    override fun validate(): List<ValidationError> {
        return validateGlueContainer.validate(this).errors
    }

    override fun toString(): String {
        return "GlueContainer(image=$image, hash=${hash.substring(0, 8)}, parameter=$parameter)"
    }
}

val validateGlueContainer = Validation {
    GlueContainer::image {
        notBlank()
    }

    GlueContainer::hash {
        notBlank()
    }
}