/*
 * Copyright 2024-2025 Fraunhofer AISEC and Fraunhofer IOSB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.fraunhofer.iosb.odrl

import com.fasterxml.jackson.annotation.*
import de.fraunhofer.iosb.odrl.glue.GlueContainerPipeline
import de.fraunhofer.iosb.odrl.glue.GlueIdentifiers
import de.fraunhofer.iosb.odrl.glue.validateGlueContainerPipeline
import de.fraunhofer.iosb.odrl.parser.JsonLd
import de.fraunhofer.iosb.odrl.parser.JsonLdBeanValidator
import io.konform.validation.Validation
import io.konform.validation.ValidationError
import io.konform.validation.jsonschema.minimum
import java.net.URI

@JsonLd(GlueIdentifiers.CONTEXT)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = OdrlConstraint::class)
@JsonTypeName(OdrlIdentifiers.CONSTRAINT)
data class OdrlConstraint(
    @param:JsonProperty("@id")
    val uid: URI? = null,
    @param:JsonProperty(OdrlIdentifiers.HAS_LEFT_OPERAND)
    val leftOperand: OdrlLeftOperand,
    @param:JsonProperty(OdrlIdentifiers.HAS_OPERATOR)
    val operator: OdrlOperator,
    @param:JsonProperty(OdrlIdentifiers.HAS_RIGHT_OPERAND)
    @param:JsonSetter(nulls = Nulls.AS_EMPTY)
    val rightOperand: List<OdrlRightOperand> = listOf(),
    // The https://www.w3.org/ns/odrl.jsonld context declares "rightOperandReference" to be of type xsd:anyUri
    @param:JsonProperty(OdrlIdentifiers.HAS_RIGHT_OPERAND_REFERENCE)
    @param:JsonSetter(nulls = Nulls.AS_EMPTY)
    val rightOperandReference: List<XmlAnyUri> = listOf(),
    // The https://www.w3.org/ns/odrl.jsonld context rewrites "dataType" to "datatype" (probably a bug?)
    @param:JsonProperty(OdrlIdentifiers.DATATYPE)
    @param:JsonAlias("http://www.w3.org/ns/odrl/2/datatype")
    val dataType: XmlAnyType? = null,
    @param:JsonProperty(OdrlIdentifiers.UNIT)
    val unit: OdrlIri? = null,
    @param:JsonProperty(OdrlIdentifiers.STATUS)
    val status: OdrlRightOperand? = null,
) : OdrlConstraintRepresentation,
    JsonLdBeanValidator {
    override fun validate(): List<ValidationError> = validateOdrlConstraint.validate(this).errors

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

    override fun toString(): String = "OdrlConstraint(${getPropertyMap().entries.joinToString(", ")})"
}

val validateOdrlConstraint =
    Validation<OdrlConstraint> {
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
        // Note: No validation of xsd:anyType necessary

        // A Constraint MAY have none or one unit property value of type IRI
        OdrlConstraint::unit ifPresent {
            run(validateOdrlIri)
        }

        // A Constraint MAY have none or one status property value for a value generated from the leftOperand action or for a value related to the leftOperand set as the reference for the comparison
        // Note: No validation of RightOperand necessary
    }
