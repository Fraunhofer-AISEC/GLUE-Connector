package fhg.glue.odrl

import com.fasterxml.jackson.databind.JsonNode

class OdrlProhibitionParser : OdrlRuleParser() {
    override val key: String = "prohibition"

    override fun parse(root: JsonNode): OdrlProhibition {
        val rule = super.parse(root)

        // A Permission MUST have one target property value of type Asset
        val target = OdrlAssetParser().parse(getFieldOrThrow(root, "target"))

        // A Permission MAY have none or one assigner and/or assignee property values of type Party
        var assigner: OdrlParty? = null
        if(root.has("assigner"))
            assigner = OdrlPartyParser().parse(root.get("assigner"))

        var assignee: OdrlParty? = null
        if(root.has("assignee"))
            assignee = OdrlPartyParser().parse(root.get("assignee"))

        // A Permission MAY have none, one, or more remedy property values of type Duty
        val remedies = mutableListOf<OdrlDuty>()
        val remedyNode = root.get("remedy")
        if (remedyNode != null) {
            if (!remedyNode.isArray) {
                throw OdrlException("Expected remedy field to be a JSON array")
            }
            for (r in remedyNode.elements()) {
                remedies.add(OdrlDutyParser().parse(r))
            }
        }

        return OdrlProhibition(rule.uid, rule.action, rule.constraints, target, assigner, assignee, remedies)
    }
}