package fhg.glue.odrl

import com.fasterxml.jackson.databind.JsonNode

class OdrlPermissionParser : OdrlRuleParser() {
    override val key: String = "permission"

    override fun parse(root: JsonNode): OdrlPermission {
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

        // A Permission MAY have none, one, or more duty property values of type Duty
        val duties = mutableListOf<OdrlDuty>()
        val dutyNode = root.get("duty")
        if (dutyNode != null) {
            if (!dutyNode.isArray) {
                throw OdrlException("Expected duty field to be a JSON array")
            }
            for (d in dutyNode.elements()) {
                duties.add(OdrlDutyParser().parse(d))
            }
        }

        return OdrlPermission(rule.uid, rule.action, rule.constraints, target, assigner, assignee, duties)
    }
}