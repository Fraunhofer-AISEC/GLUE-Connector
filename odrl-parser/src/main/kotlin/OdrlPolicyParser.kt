package fhg.glue.odrl

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.JsonNode
import com.github.jsonldjava.core.JsonLdError
import com.github.jsonldjava.core.JsonLdOptions
import com.github.jsonldjava.core.JsonLdProcessor
import com.github.jsonldjava.utils.JsonUtils

class OdrlPolicyParser : OdrlParser() {
    override val key = "policy"

    fun parseJsonLd(policy: String): OdrlPolicy {
        try {
            // Compact JSON-LD
            val compacted = JsonLdProcessor.compact(JsonUtils.fromString(policy), mapOf<Any?, Any?>(), JsonLdOptions());
            // Parse compacted policy
            return parse(ObjectMapper().readTree(JsonUtils.toPrettyString(compacted)))
        } catch (e: JsonLdError) {
            throw OdrlException("Failed to compact JSON-LD of ODRL policy!", e)
        } catch (e: JsonParseException) {
            throw OdrlException("Failed to parse JSON of ODRL policy!", e)
        } catch (e: JsonProcessingException) {
            throw OdrlException("Failed to process JSON of ODRL policy!", e)
        }
    }

    fun parse(root: JsonNode): OdrlPolicy {
        if (!root.isObject)
            throw OdrlException("Expected ODRL policy to be a JSON object")

        // Only ODRL policies supported
        var type = OdrlIdentifiers.SET // "The Set Policy subclass is the default subclass of Policy" (https://www.w3.org/TR/odrl-model)
        if (root.has("@type")) {
            type = getStringOrThrow(root, "@type")
        }
        if (root.has(RdfIdentifiers.TYPE)) {
            type = RdfTypeParser().parse(root.get(RdfIdentifiers.TYPE))
        }
        if (type != OdrlIdentifiers.SET && type != OdrlIdentifiers.POLICY)
            throw OdrlException("Expected ODRL policy to be of type 'Set' or 'Policy'")

        // A Policy MUST have one uid property value of type IRI
        val uid = if (root.has(OdrlIdentifiers.UID))  getIriOrThrow(root, OdrlIdentifiers.UID)
                    else getIriOrThrow(root, "@id")

        // A Policy MUST have at least one permission, prohibition, or obligation property values
        if (!root.has(OdrlIdentifiers.HAS_PERMISSION) && !root.has(OdrlIdentifiers.HAS_PROHIBITION) && !root.has(OdrlIdentifiers.OBLIGATION)) {
            throw OdrlException("Expected policy to have at least one of: 'permission', 'prohibition' or 'obligation'")
        }

        val permissions = mutableListOf<OdrlPermission>()
        val permissionNode = root.get(OdrlIdentifiers.HAS_PERMISSION)
        if (permissionNode != null) {
            if (permissionNode.isObject) {
                permissions.add(OdrlPermissionParser().parse(permissionNode))
            } else if (permissionNode.isArray) {
                for (p in permissionNode.elements())
                    permissions.add(OdrlPermissionParser().parse(p))
            } else {
                throw OdrlException("Expected field 'permission' to be a JSON object or array")
            }
        }

        val prohibitions = mutableListOf<OdrlProhibition>()
        val prohibitionNode = root.get(OdrlIdentifiers.HAS_PROHIBITION)
        if (prohibitionNode != null) {
            if (prohibitionNode.isObject) {
                prohibitions.add(OdrlProhibitionParser().parse(prohibitionNode))
            } else if (prohibitionNode.isArray) {
                for (p in prohibitionNode.elements())
                    prohibitions.add(OdrlProhibitionParser().parse(p))
            } else {
                throw OdrlException("Expected field 'prohibition' to be a JSON object or array")
            }
        }

        val obligations = mutableListOf<OdrlDuty>()
        val obligationNode = root.get(OdrlIdentifiers.OBLIGATION)
        if (obligationNode != null) {
            if (obligationNode.isObject) {
                obligations.add(OdrlDutyParser().parse(obligationNode))
            } else if (obligationNode.isArray) {
                for (p in obligationNode.elements())
                    obligations.add(OdrlDutyParser().parse(p))
            } else {
                throw OdrlException("Expected field 'obligation' to be a JSON object or array")
            }
        }

        // A Policy MAY have none, one, or many profile property values
        val profiles = mutableListOf<String>()
        val profileNode = root.get(OdrlIdentifiers.PROFILE)
        if (profileNode != null) {
            if (profileNode.isTextual) {
                profiles.add(profileNode.asText())
            } else if (profileNode.isObject) {
                profiles.add(getStringOrThrow(profileNode, "@id"))
            } else if (profileNode.isArray) {
                for (p in profileNode.elements()) {
                    if (!p.isTextual)
                        throw OdrlException("Expected field 'profile' to be a string or array of strings")
                    profiles.add(p.asText())
                }
            } else {
                throw OdrlException("Expected field 'profile' to be a string or array of strings")
            }
        }


        // A Policy MAY have none or one conflict property values
        var conflict = OdrlIdentifiers.INVALID
        val conflictNode = root.get(OdrlIdentifiers.CONFLICT)
        if (conflictNode != null) {
            if (conflictNode.isTextual)
                conflict = conflictNode.asText()
            else if (conflictNode.isObject)
                getStringOrThrow(conflictNode, "@id")
            else
                throw OdrlException("Expected field 'conflict' to be a string")
        }
        if (conflict != OdrlIdentifiers.INVALID && conflict != OdrlIdentifiers.PERM && conflict != OdrlIdentifiers.PROHIBIT)
            throw OdrlException("Expected field 'conflict' to be one of 'invalid', 'perm' or 'prohibit'")

        return OdrlPolicy(uid, permissions, prohibitions, obligations, profiles, conflict)
    }

}