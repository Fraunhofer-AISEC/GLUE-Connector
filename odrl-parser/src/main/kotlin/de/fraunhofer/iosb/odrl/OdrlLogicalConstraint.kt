package de.fraunhofer.iosb.odrl

import com.fasterxml.jackson.annotation.*
import de.fraunhofer.iosb.odrl.glue.GlueIdentifiers
import de.fraunhofer.iosb.odrl.parser.JsonLd
import de.fraunhofer.iosb.odrl.parser.JsonLdBeanValidator
import io.konform.validation.Validation
import io.konform.validation.ValidationError
import io.konform.validation.jsonschema.minimum
import java.net.URI

@JsonLd(GlueIdentifiers.CONTEXT)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = OdrlLogicalConstraint::class)
@JsonTypeName(OdrlIdentifiers.LOGICAL_CONSTRAINT)
data class OdrlLogicalConstraint(
    @param:JsonProperty("@id")
    val uid: URI? = null,

    @param:JsonProperty(OdrlIdentifiers.OR)
    @param:JsonSetter(nulls = Nulls.AS_EMPTY)
    val or: List<OdrlConstraint> = listOf(),

    @param:JsonProperty(OdrlIdentifiers.XONE)
    @param:JsonSetter(nulls = Nulls.AS_EMPTY)
    val xone: List<OdrlConstraint> = listOf(),

    @param:JsonProperty(OdrlIdentifiers.AND)
    @param:JsonSetter(nulls = Nulls.AS_EMPTY)
    val and: List<OdrlConstraint> = listOf(),

    @param:JsonProperty(OdrlIdentifiers.AND_SEQUENCE)
    @param:JsonSetter(nulls = Nulls.AS_EMPTY)
    val andSequence: List<OdrlConstraint> = listOf(),
): OdrlConstraintRepresentation, JsonLdBeanValidator {
    override fun validate(): List<ValidationError> {
        return validateOdrlLogicalConstraint.validate(this).errors
    }

    private fun getPropertyMap(): MutableMap<String, Any> {
        val map = mutableMapOf<String, Any>()
        if (uid != null) map["uid"] = uid
        if (or.isNotEmpty()) map["or"] = or
        if (xone.isNotEmpty()) map["xone"] = xone
        if (and.isNotEmpty()) map["and"] = and
        if (andSequence.isNotEmpty()) map["andSequence"] = andSequence
        return map
    }

    override fun toString(): String {
        return "OdrlLogicalConstraint(${getPropertyMap().entries.joinToString(", ")})"
    }
}

val validateOdrlLogicalConstraint = Validation<OdrlLogicalConstraint> {
    // A Logical Constraint MAY have none or one uid property value of type IRI
    OdrlLogicalConstraint::uid ifPresent {
        addConstraint("OdrlLogicalConstraint: {value} is not a valid IRI") { isIri(it) }
    }

    // A Logical Constraint MUST have one operand sub-property indicating the logical relationship of the compared existing constraints
    addConstraint("OdrlLogicalConstraint: Use exactly ONE operand of 'or', 'xone', 'and', 'andSequence'") {
        (if (it.or.isEmpty()) 0 else 1)  + (if (it.xone.isEmpty()) 0 else 1) + (if (it.and.isEmpty()) 0 else 1) + (if (it.andSequence.isEmpty()) 0 else 1) == 1
    }
    validate("logicalConstraintOperandNotEmpty", { it.or.size + it.xone.size + it.and.size + it.andSequence.size }) {
        minimum(1)
    }

    // All of the operand values MUST be unique Constraint instances
    OdrlLogicalConstraint::or onEach {
        run(validateOdrlConstraint)
    }
    OdrlLogicalConstraint::xone onEach {
        run(validateOdrlConstraint)
    }
    OdrlLogicalConstraint::and onEach {
        run(validateOdrlConstraint)
    }
    OdrlLogicalConstraint::andSequence onEach {
        run(validateOdrlConstraint)
    }
}