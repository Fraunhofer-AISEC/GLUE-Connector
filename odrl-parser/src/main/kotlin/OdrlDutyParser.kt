package fhg.glue.odrl

import com.fasterxml.jackson.databind.JsonNode

class OdrlDutyParser : OdrlRuleParser() {
    override val key: String = "duty"

    override fun parse(root: JsonNode): OdrlDuty {
        val rule = super.parse(root)

        // A Duty MAY have none or one target property values of type Asset
        var target: OdrlAsset? = null
        if (root.has("target"))
            target = OdrlAssetParser().parse(root.get("target"))

        // A Duty MAY have none or one assigner and/or assignee property values of type Party
        var assigner: OdrlParty? = null
        if(root.has("assigner"))
            assigner = OdrlPartyParser().parse(root.get("assigner"))

        var assignee: OdrlParty? = null
        if(root.has("assignee"))
            assignee = OdrlPartyParser().parse(root.get("assignee"))


        return OdrlDuty(rule.uid, rule.action, rule.constraints, target, assigner, assignee)
    }
}