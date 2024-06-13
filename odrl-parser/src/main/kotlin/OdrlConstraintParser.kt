package fhg.glue.odrl

import com.fasterxml.jackson.databind.JsonNode

open class OdrlConstraintParser : OdrlParser() {
    override val key: String = "constraint"

    open fun parse(root: JsonNode): OdrlConstraint {
        if (!root.isObject)
            throw OdrlException("Expected constraint to be a JSON object")

        // A Constraint MAY have none or one uid property value of type IRI
        var uid: String? = null;
        if (root.has("uid"))
            uid = getIriOrThrow(root, "uid")

        // A Constraint MUST have one leftOperand property value of type LeftOperand
        val leftOperand = OdrlLeftOperandParser().parse(getFieldOrThrow(root, "leftOperand"))

        // A Constraint MUST have one operator property value of type Operator
        val operator = OdrlOperatorParser().parse(getFieldOrThrow(root, "operator"))

        // A Constraint MUST have EITHER one rightOperand property value of type
        //      literal, or IRI, or RightOperand; or
        //      for set-based operators; list of literals, or list of IRIs, or list of RightOperands;
        val rightOperand = OdrlLiteralParser().parse(getFieldOrThrow(root, "rightOperand"))





        return OdrlConstraint(uid, leftOperand, operator, rightOperand)
    }
}