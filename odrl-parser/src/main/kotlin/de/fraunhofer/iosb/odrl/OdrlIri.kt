package fhg.glue.odrl.de.fraunhofer.iosb.odrl

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import fhg.glue.odrl.de.fraunhofer.iosb.odrl.glue.GlueIdentifiers
import fhg.glue.odrl.de.fraunhofer.iosb.odrl.parser.JsonLd
import fhg.glue.odrl.de.fraunhofer.iosb.odrl.parser.JsonLdBeanValidator
import io.konform.validation.Validation
import io.konform.validation.ValidationError
import java.net.URI

@JsonLd(GlueIdentifiers.CONTEXT)
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE, defaultImpl = OdrlIri::class)
data class OdrlIri(
    @JsonProperty("@id")
    val uid: URI,
): JsonLdBeanValidator {
    constructor(primitiveValue: String) : this(URI.create(primitiveValue))

    override fun validate(): List<ValidationError> {
        return validateOdrlIri.validate(this).errors
    }
}

fun isIri(uri: URI): Boolean {
    return true
}

val validateOdrlIri = Validation {
    OdrlIri::uid {
        addConstraint("OdrlIri: {value} is not a valid IRI") { isIri(it) }
    }
}