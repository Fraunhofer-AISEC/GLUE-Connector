package fhg.glue.odrl

import com.fasterxml.jackson.databind.JsonNode

class OdrlDutyParser : OdrlRuleParser() {
    override val key: String = "duty"

    override fun parse(root: JsonNode): OdrlDuty {
        val rule = super.parse(root)

        // A Duty MAY have none or one target property values of type Asset
        var target: OdrlAsset? = null
        if (root.has(OdrlIdentifiers.TARGET))
            target = OdrlAssetParser().parse(root.get(OdrlIdentifiers.TARGET))

        // A Duty MAY have none or one assigner and/or assignee property values of type Party
        var assigner: OdrlParty? = null
        if(root.has(OdrlIdentifiers.ASSIGNER))
            assigner = OdrlPartyParser().parse(root.get(OdrlIdentifiers.ASSIGNER))

        var assignee: OdrlParty? = null
        if(root.has(OdrlIdentifiers.ASSIGNEE))
            assignee = OdrlPartyParser().parse(root.get(OdrlIdentifiers.ASSIGNEE))


        return OdrlDuty(rule.uid, rule.action, rule.constraints, target, assigner, assignee)
    }
}