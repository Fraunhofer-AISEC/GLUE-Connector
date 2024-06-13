package fhg.glue.odrl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.JsonNode

class OdrlPolicyParser : OdrlParser() {
    override val key = "policy"

    fun parseJson(policy: String): OdrlPolicy {
        return parse(ObjectMapper().readTree(policy))
    }

    fun parse(root: JsonNode): OdrlPolicy {
        if (!root.isObject)
            throw OdrlException("Expected ODRL policy to be a JSON object")

        if (root.has("@type")) {
            val type = getStringOrThrow(root, "@type")
            if (type != "Set" && type != "Policy")
                throw OdrlException("Expected ODRL policy to be of type 'Set' or 'Policy'")
        }

        // A Policy MUST have one uid property value of type IRI
        val uid = getIriOrThrow(root, "uid")

        // A Policy MUST have at least one permission, prohibition, or obligation property values
        if (!root.has("permission") && !root.has("prohibition") && !root.has("obligation")) {
            throw OdrlException("Expected policy to have at least one of: 'permission', 'prohibition' or 'obligation'")
        }

        val permissions = mutableListOf<OdrlPermission>()
        val permissionNode = root.get("permission")
        if (permissionNode != null) {
            if (!permissionNode.isArray) {
                throw OdrlException("Expected field 'permission' to be a JSON array")
            }
            for (p in permissionNode.elements()) {
                permissions.add(OdrlPermissionParser().parse(p))
            }
        }

        val prohibitions = mutableListOf<OdrlProhibition>()
        val prohibitionNode = root.get("prohibition")
        if (prohibitionNode != null) {
            if (!prohibitionNode.isArray) {
                throw OdrlException("Expected field 'prohibition' to be a JSON array")
            }
            for (p in prohibitionNode.elements()) {
                prohibitions.add(OdrlProhibitionParser().parse(p))
            }
        }

        val obligations = mutableListOf<OdrlDuty>()
        val obligationNode = root.get("obligation")
        if (obligationNode != null) {
            if (!obligationNode.isArray) {
                throw OdrlException("Expected field 'obligation' to be a JSON array")
            }
            for (p in obligationNode.elements()) {
                obligations.add(OdrlDutyParser().parse(p))
            }
        }

        // A Policy MAY have none, one, or many profile property values
        val profiles = mutableListOf<String>()
        val profileNode = root.get("profile")
        if (profileNode != null) {
            if (profileNode.isTextual)
                profiles.add(profileNode.asText())
            else if (profileNode.isArray)
                for (p in profileNode.elements()) {
                    if (!p.isTextual)
                        throw OdrlException("Expected field 'profile' to be a string or array of strings")
                    profiles.add(p.asText())
                }
            else
                throw OdrlException("Expected field 'profile' to be a string or array of strings")
        }


        // A Policy MAY have none or one conflict property values
        var conflict = "invalid"
        if (root.has("conflict"))
            conflict = getStringOrThrow(root, "conflict")
        if (conflict != "invalid" && conflict != "perm" && conflict != "prohibit")
            throw OdrlException("Expected field 'conflict' to be one of 'invalid', 'perm' or 'prohibit'")

        return OdrlPolicy(uid, permissions, prohibitions, obligations, profiles, conflict)
    }

}