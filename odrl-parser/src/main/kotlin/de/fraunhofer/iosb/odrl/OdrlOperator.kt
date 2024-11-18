package fhg.glue.odrl.de.fraunhofer.iosb.odrl

import com.fasterxml.jackson.annotation.*
import fhg.glue.odrl.de.fraunhofer.iosb.odrl.glue.GlueIdentifiers
import fhg.glue.odrl.de.fraunhofer.iosb.odrl.parser.JsonLd
import fhg.glue.odrl.de.fraunhofer.iosb.odrl.parser.JsonLdBeanValidator
import io.konform.validation.Validation
import io.konform.validation.ValidationError
import java.net.URI

@JsonLd(GlueIdentifiers.CONTEXT)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = OdrlOperator::class)
@JsonTypeName(OdrlIdentifiers.OPERATOR)
data class OdrlOperator(
    @JsonProperty("@id")
    val uid: URI,
): JsonLdBeanValidator {
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