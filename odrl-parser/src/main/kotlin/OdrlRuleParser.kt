package fhg.glue.odrl

import com.fasterxml.jackson.databind.JsonNode

open class OdrlRuleParser : OdrlParser() {
    override val key: String = "rule"

    open fun parse(root: JsonNode): OdrlRule {
        if (!root.isObject)
            throw OdrlException("Expected rule to be a JSON object")

        // A Rule MUST have one action property value of type Action
        val action = OdrlActionParser().parse(getFieldOrThrow(root, "action"))

        // A Rule MAY have none or one uid property values of type IRI
        var uid: String? = null;
        if (root.has("uid"))
            uid = getIriOrThrow(root, "uid")

        // A Rule MAY have none, one or many constraint property values of type Constraint
        val constraints = mutableListOf<OdrlConstraint>()
        val constraintNode = root.get("constraint")
        if (constraintNode != null) {
            if (!constraintNode.isArray) {
                throw OdrlException("Expected constraint field to be a JSON array")
            }
            for (c in constraintNode.elements()) {
                constraints.add(OdrlConstraintParser().parse(c))
            }
        }




        return OdrlRule(uid, action, constraints)
    }
}