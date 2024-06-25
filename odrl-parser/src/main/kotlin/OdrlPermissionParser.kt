package fhg.glue.odrl

import com.fasterxml.jackson.databind.JsonNode

class OdrlPermissionParser : OdrlRuleParser() {
    override val key: String = "permission"

    override fun parse(root: JsonNode): OdrlPermission {
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

        // A Permission MAY have none, one, or more duty property values of type Duty
        val duties = mutableListOf<OdrlDuty>()
        val dutyNode = root.get(OdrlIdentifiers.HAS_DUTY)
        if (dutyNode != null) {
            if (dutyNode.isObject) {
                duties.add(OdrlDutyParser().parse(dutyNode))
            } else if (dutyNode.isArray) {
                for (d in dutyNode.elements())
                    duties.add(OdrlDutyParser().parse(d))
            } else {
                throw OdrlException("Expected field 'duty' to be a JSON object or array")
            }
        }

        return OdrlPermission(rule.uid, rule.action, rule.constraints, target, assigner, assignee, duties)
    }
}