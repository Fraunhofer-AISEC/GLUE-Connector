package de.fraunhofer.aisec.glue.negotiation

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import de.fraunhofer.iosb.odrl.*

fun OdrlOffer.toAgreement(mapper: ObjectMapper, assignee: OdrlParty): OdrlAgreement {
    val offerJson = mapper.writeValueAsString(this)
    val offerMap = mapper.readValue(offerJson, Map::class.java).toMutableMap()
    val assignerJson = mapper.convertValue(assignee, JsonNode::class.java)
    offerMap.put(OdrlIdentifiers.ASSIGNEE, assignerJson)
    offerMap.put("type", "http://www.w3.org/ns/odrl/2/Agreement")
    return mapper.convertValue(offerMap, OdrlAgreement::class.java)
}

/**
 * Convert any policy type into a base policy, stripping out the assigner and assignee fields added by its known
 * subtypes [OdrlOffer] and [OdrlAgreement].
 */
fun OdrlPolicy.upcast(mapper: ObjectMapper): OdrlPolicy {
    return when (this) {
        is OdrlOffer, is OdrlAgreement -> {
            // TODO: Can I get this to serialize to the expanded form somehow?
            val valuesJson = mapper.writeValueAsString(this)
            val valuesMap = mapper.readValue<Map<String, Any>>(valuesJson).toMutableMap()
            valuesMap["type"] = OdrlIdentifiers.POLICY
            valuesMap.remove("assigner")
            valuesMap.remove("assignee")
            val policy = mapper.convertValue<OdrlPolicy>(valuesMap)
            policy
        }
        else -> this
    }
}
