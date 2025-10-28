package de.fraunhofer.iosb.odrl

import com.fasterxml.jackson.annotation.*
import de.fraunhofer.iosb.odrl.glue.GlueIdentifiers
import de.fraunhofer.iosb.odrl.parser.JsonLd
import de.fraunhofer.iosb.odrl.parser.JsonLdBeanValidator
import io.konform.validation.Validation
import io.konform.validation.ValidationError
import java.net.URI

@JsonLd(GlueIdentifiers.CONTEXT)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = OdrlConflictTerm::class)
@JsonTypeName(OdrlIdentifiers.CONFLICT_TERM)
data class OdrlConflictTerm(
    @param:JsonProperty("@id")
    val uid: URI,
): JsonLdBeanValidator {
    // Constructor necessary for Jackson to deserialize primitive type
    @Suppress("unused")
    constructor(primitiveValue: String) : this(URI.create(primitiveValue))

    override fun validate(): List<ValidationError> {
        return validateOdrlConflictTerm.validate(this).errors
    }

    fun isInvalid(): Boolean {
        return uid.toString() == OdrlIdentifiers.INVALID
    }

    fun isProhibit(): Boolean {
        return uid.toString() == OdrlIdentifiers.PROHIBIT
    }

    fun isPerm(): Boolean {
        return uid.toString() == OdrlIdentifiers.PERM
    }
}

val validateOdrlConflictTerm = Validation<OdrlConflictTerm> {
    addConstraint("OdrlConflictTerm: {value} must be one of 'invalid', 'prohibit', or 'perm'") {
        it.isInvalid() || it.isProhibit() || it.isPerm()
    }
}