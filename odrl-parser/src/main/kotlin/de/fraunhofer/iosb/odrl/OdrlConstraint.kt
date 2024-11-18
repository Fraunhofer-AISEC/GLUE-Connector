package fhg.glue.odrl.de.fraunhofer.iosb.odrl

import com.fasterxml.jackson.annotation.*
import fhg.glue.odrl.de.fraunhofer.iosb.odrl.glue.GlueContainerPipeline
import fhg.glue.odrl.de.fraunhofer.iosb.odrl.glue.GlueIdentifiers
import fhg.glue.odrl.de.fraunhofer.iosb.odrl.glue.validateGlueContainerPipeline
import fhg.glue.odrl.de.fraunhofer.iosb.odrl.parser.JsonLd
import fhg.glue.odrl.de.fraunhofer.iosb.odrl.parser.JsonLdBeanValidator
import io.konform.validation.Validation
import io.konform.validation.ValidationError
import io.konform.validation.jsonschema.minimum
import java.net.URI

@JsonLd(GlueIdentifiers.CONTEXT)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = OdrlConstraint::class)
@JsonTypeName(OdrlIdentifiers.CONSTRAINT)
data class OdrlConstraint(
    @JsonProperty("@id")
    val uid: URI? = null,

    @JsonProperty(OdrlIdentifiers.HAS_LEFT_OPERAND)
    val leftOperand: OdrlLeftOperand,

    @JsonProperty(OdrlIdentifiers.HAS_OPERATOR)
    val operator: OdrlOperator,

    @JsonProperty(OdrlIdentifiers.HAS_RIGHT_OPERAND)
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    val rightOperand: List<OdrlRightOperand> = listOf(),

    @JsonProperty(OdrlIdentifiers.HAS_RIGHT_OPERAND_REFERENCE)
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    val rightOperandReference: List<OdrlIri> = listOf(),

    @JsonProperty(OdrlIdentifiers.DATATYPE)
    val dataType: OdrlIri? = null,

    @JsonProperty(OdrlIdentifiers.UNIT)
    val unit: OdrlIri? = null,

    @JsonProperty(OdrlIdentifiers.STATUS)
    val status: OdrlRightOperand? = null,
): OdrlConstraintRepresentation, JsonLdBeanValidator {
    override fun validate(): List<ValidationError> {
        return validateOdrlConstraint.validate(this).errors
    }

    private fun getPropertyMap(): MutableMap<String, Any> {
        val map = mutableMapOf<String, Any>()
        if (uid != null) map["uid"] = uid
        map["leftOperand"] = leftOperand
        map["operator"] = operator
        if (rightOperand.isNotEmpty()) map["rightOperand"] = rightOperand
        if (rightOperandReference.isNotEmpty()) map["rightOperandReference"] = rightOperandReference
        if (dataType != null) map["dataType"] = dataType
        if (unit != null) map["unit"] = unit
        if (status != null) map["status"] = status
        return map
    }

    override fun toString(): String {
        return "OdrlConstraint(${getPropertyMap().entries.joinToString(", ")})"
    }
}

val validateOdrlConstraint = Validation<OdrlConstraint> {
    // A Constraint MAY have none or one uid property value of type IRI
    OdrlConstraint::uid ifPresent {
        addConstraint("OdrlConstraint: {value} is not a valid IRI") { isIri(it) }
    }

    // A Constraint MUST have one leftOperand property value of type LeftOperand
    OdrlConstraint::leftOperand {
        run(validateOdrlLeftOperand)
    }

    // A Constraint MUST have one operator property value of type Operator
    OdrlConstraint::operator {
        run(validateOdrlOperator)
    }

    // A Constraint MUST have either:
    //   - One rightOperand property value of type
    //       (i) literal, or IRI, or RightOperand, or
    //       (ii) for set-based operators: list of literals, or list of IRIs, or list of RightOperands
    //   - One rightOperandReference property value of type
    //       (i) IRI, or
    //       (ii) for set-based operators: list of IRIs
    addConstraint("OdrlConstraint: Use either 'rightOperand' OR 'rightOperandReference'") {
        it.rightOperand.isEmpty() || it.rightOperandReference.isEmpty()
    }
    validate("constraintRightOperandNotEmpty", { it.rightOperand.size + it.rightOperandReference.size }) {
        minimum(1)
    }
    OdrlConstraint::rightOperand onEach {
        ifInstanceOf<GlueContainerPipeline> {
            run(validateGlueContainerPipeline)
        }
    }

    // A Constraint MAY have none or one dataType property value for the data type of the rightOperand/Reference
    OdrlConstraint::dataType ifPresent {
        run(validateOdrlIri)
    }

    // A Constraint MAY have none or one unit property value of type IRI
    OdrlConstraint::unit ifPresent {
        run(validateOdrlIri)
    }

    // A Constraint MAY have none or one status property value for a value generated from the leftOperand action or for a value related to the leftOperand set as the reference for the comparison
    // Note: No validation of RightOperand necessary
}