package fhg.glue.odrl

import com.fasterxml.jackson.databind.JsonNode

open class OdrlRuleParser : OdrlParser() {
    override val key: String = "rule"

    open fun parse(root: JsonNode): OdrlRule {
        if (!root.isObject)
            throw OdrlException("Expected rule to be a JSON object")

        // A Rule MUST have one action property value of type Action
        val action = OdrlActionParser().parse(getFieldOrThrow(root, OdrlIdentifiers.HAS_ACTION))

        // A Rule MAY have none or one uid property values of type IRI
        var uid: String? = null;
        if (root.has(OdrlIdentifiers.UID))
            uid = getIriOrThrow(root, OdrlIdentifiers.UID)
        else if (root.has("@id"))
            uid = getIriOrThrow(root, "@id")

        // A Rule MAY have none, one or many constraint property values of type Constraint
        val constraints = mutableListOf<OdrlConstraint>()
        val constraintNode = root.get(OdrlIdentifiers.HAS_CONSTRAINT)
        if (constraintNode != null) {
            if (constraintNode.isObject) {
                constraints.add(OdrlConstraintParser().parse(constraintNode))
            } else if (constraintNode.isArray) {
                for (c in constraintNode.elements())
                    constraints.add(OdrlConstraintParser().parse(c))
            } else {
                throw OdrlException("Expected field 'constraint' to be a JSON object or array")
            }
        }




        return OdrlRule(uid, action, constraints)
    }
}