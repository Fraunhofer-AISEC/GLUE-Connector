package fhg.glue.odrl

import com.fasterxml.jackson.databind.JsonNode

open class OdrlConstraintParser : OdrlParser() {
    override val key: String = "constraint"

    open fun parse(root: JsonNode): OdrlConstraint {
        if (!root.isObject)
            throw OdrlException("Expected constraint to be a JSON object")

        // A Constraint MAY have none or one uid property value of type IRI
        var uid: String? = null;
        if (root.has(OdrlIdentifiers.UID))
            uid = getIriOrThrow(root, OdrlIdentifiers.UID)
        else if (root.has("@id"))
            uid = getIriOrThrow(root, "@id")

        // A Constraint MUST have one leftOperand property value of type LeftOperand
        val leftOperand = OdrlLeftOperandParser().parse(getFieldOrThrow(root, OdrlIdentifiers.HAS_LEFTOPERAND))

        // A Constraint MUST have one operator property value of type Operator
        val operator = OdrlOperatorParser().parse(getFieldOrThrow(root, OdrlIdentifiers.HAS_OPERATOR))

        // A Constraint MUST have EITHER one rightOperand property value of type
        //      literal, or IRI, or RightOperand; or
        //      for set-based operators; list of literals, or list of IRIs, or list of RightOperands;
        val rightOperand = OdrlLiteralParser().parse(getFieldOrThrow(root, OdrlIdentifiers.HAS_RIGHTOPERAND))





        return OdrlConstraint(uid, leftOperand, operator, rightOperand)
    }
}