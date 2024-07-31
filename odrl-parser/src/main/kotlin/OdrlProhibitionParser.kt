package fhg.glue.odrl

import com.fasterxml.jackson.databind.JsonNode

class OdrlProhibitionParser : OdrlRuleParser() {
    override val key: String = "prohibition"

    override fun parse(root: JsonNode): OdrlProhibition {
        val rule = super.parse(root)

        // A Permission MUST have one target property value of type Asset
        val target = OdrlAssetParser().parse(getFieldOrThrow(root, OdrlIdentifiers.TARGET))

        // A Permission MAY have none or one assigner and/or assignee property values of type Party
        var assigner: OdrlParty? = null
        if(root.has(OdrlIdentifiers.ASSIGNER))
            assigner = OdrlPartyParser().parse(root.get(OdrlIdentifiers.ASSIGNER))

        var assignee: OdrlParty? = null
        if(root.has(OdrlIdentifiers.ASSIGNEE))
            assignee = OdrlPartyParser().parse(root.get(OdrlIdentifiers.ASSIGNEE))

        // A Permission MAY have none, one, or more remedy property values of type Duty
        val remedies = mutableListOf<OdrlDuty>()
        val remedyNode = root.get(OdrlIdentifiers.REMEDY)
        if (remedyNode != null) {
            if (remedyNode.isObject) {
                remedies.add(OdrlDutyParser().parse(remedyNode))
            } else if (remedyNode.isArray) {
                for (r in remedyNode.elements())
                    remedies.add(OdrlDutyParser().parse(r))
            } else {
                throw OdrlException("Expected field 'remedy' to be a JSON object or array")
            }
        }

        return OdrlProhibition(rule.uid, rule.action, rule.constraints, target, assigner, assignee, remedies)
    }
}