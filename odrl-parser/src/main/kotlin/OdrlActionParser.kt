package fhg.glue.odrl

import com.fasterxml.jackson.databind.JsonNode

class OdrlActionParser: OdrlParser() {
    override val key = "action"

    fun parse(root: JsonNode): OdrlAction {
        if (root.isTextual) {
            isIriOrThrow(root.asText())
            return OdrlAction(root.asText(), listOf())
        }

        if (!root.isObject)
            throw OdrlException("Expected action to be a string or object")

        val uid = getIriOrThrow(root, "@id")

        // An Action MAY have none, one or more refinement property values of type Constraint
        val refinements = mutableListOf<OdrlConstraint>()
        val refinementNode = root.get(OdrlIdentifiers.REFINEMENT)
        if (refinementNode != null) {
            if (refinementNode.isObject) {
                refinements.add(OdrlConstraintParser().parse(refinementNode))
            } else if (refinementNode.isArray) {
                for (r in refinementNode.elements())
                    refinements.add(OdrlConstraintParser().parse(r))
            } else {
                throw OdrlException("Expected field 'refinement' to be a JSON object or array")
            }
        }

        return OdrlAction(uid, refinements)
    }
}