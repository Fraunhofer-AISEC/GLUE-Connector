package de.fraunhofer.iosb.odrl

import com.fasterxml.jackson.annotation.*
import de.fraunhofer.iosb.odrl.glue.GlueIdentifiers
import de.fraunhofer.iosb.odrl.parser.JsonLd
import de.fraunhofer.iosb.odrl.parser.JsonLdBeanValidator
import io.konform.validation.Validation
import io.konform.validation.ValidationError
import java.net.URI

@JsonLd(GlueIdentifiers.CONTEXT)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = OdrlAction::class)
@JsonTypeName(OdrlIdentifiers.ACTION)
data class OdrlAction(
    @JsonProperty("@id")
    val uid: URI,

    @JsonProperty(OdrlIdentifiers.REFINEMENT)
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    val refinement: List<OdrlConstraintRepresentation> = listOf(),

    @JsonProperty(OdrlIdentifiers.INCLUDED_IN)
    val includedIn: OdrlAction? = null,

    @JsonProperty(OdrlIdentifiers.IMPLIES)
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    val implies: List<OdrlAction> = listOf(),
): JsonLdBeanValidator {
    constructor(primitiveValue: String) : this(URI.create(primitiveValue))

    override fun validate(): List<ValidationError> {
        return validateOdrlAction.validate(this).errors
    }

    private fun getPropertyMap(): MutableMap<String, Any> {
        val map = mutableMapOf<String, Any>()
        map["uid"] = uid
        if (refinement.isNotEmpty()) map["refinement"] = refinement
        if (includedIn != null) map["includedIn"] = includedIn
        if (implies.isNotEmpty()) map["implies"] = implies
        return map
    }

    override fun toString(): String {
        return "OdrlAction(${getPropertyMap().entries.joinToString(", ")})"
    }
}

val validateOdrlAction = Validation {
    OdrlAction::uid {
        addConstraint("OdrlAction: {value} is not a valid IRI") { isIri(it) }
    }

    // An Action MAY have none, one or more refinement property values of type Constraint
    OdrlAction::refinement {
        addConstraint("OdrlAction: Refinement list must be of homogeneous types Constraint/LogicalConstraint") { list ->
            list.all { it is OdrlConstraint } || list.all { it is OdrlLogicalConstraint }
        }
    }
    OdrlAction::refinement onEach {
        ifInstanceOf<OdrlConstraint> {
            run(validateOdrlConstraint)
        }
        ifInstanceOf<OdrlLogicalConstraint> {
            run(validateOdrlLogicalConstraint)
        }
    }

    // An Action (except for use and transfer) MUST have one includedIn property value of type Action
    // Note: This property may be defined in the vocabulary instead of the JSONLD object
    OdrlAction::includedIn ifPresent {
        addConstraint("OdrlAction: Invalid action in property 'includedIn'") { validateRecursively(it) }
    }

    // An Action MAY have none, one or more implies property values of type Action
    OdrlAction::implies onEach {
        addConstraint("OdrlAction: Invalid action in property 'implies'") { validateRecursively(it) }
    }
}

private fun validateRecursively(action: OdrlAction): Boolean {
    return validateOdrlAction.validate(action).isValid
}
