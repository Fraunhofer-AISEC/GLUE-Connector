package de.fraunhofer.iosb.odrl

import com.fasterxml.jackson.annotation.*
import de.fraunhofer.iosb.odrl.glue.GlueIdentifiers
import de.fraunhofer.iosb.odrl.parser.JsonLd
import de.fraunhofer.iosb.odrl.parser.JsonLdBeanValidator
import io.konform.validation.Validation
import io.konform.validation.ValidationError
import java.net.URI

@JsonLd(GlueIdentifiers.CONTEXT)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = OdrlOperator::class)
@JsonTypeName(OdrlIdentifiers.OPERATOR)
data class OdrlOperator(
    @param:JsonProperty("@id")
    val uid: URI,
): JsonLdBeanValidator {
    // Constructor necessary for Jackson to deserialize primitive type
    @Suppress("unused")
    constructor(primitiveValue: String) : this(URI.create(primitiveValue))

    override fun validate(): List<ValidationError> {
        return validateOdrlOperator.validate(this).errors
    }
}

val validateOdrlOperator = Validation {
    OdrlOperator::uid {
        addConstraint("OdrlOperator: {value} is not a valid IRI") { isIri(it) }
    }
}