package de.fraunhofer.aisec.glue.negotiation

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import de.fraunhofer.iosb.odrl.OdrlAgreement
import de.fraunhofer.iosb.odrl.OdrlIdentifiers
import de.fraunhofer.iosb.odrl.OdrlOffer
import de.fraunhofer.iosb.odrl.OdrlParty

fun OdrlOffer.toAgreement(mapper: ObjectMapper, assignee: OdrlParty): OdrlAgreement {
    val offerJson = mapper.writeValueAsString(this)
    val offerMap = mapper.readValue(offerJson, Map::class.java).toMutableMap()
    val assignerJson = mapper.convertValue(assignee, JsonNode::class.java)
    offerMap.put(OdrlIdentifiers.ASSIGNEE, assignerJson)
    offerMap.put("type", "http://www.w3.org/ns/odrl/2/Agreement")
    return mapper.convertValue(offerMap, OdrlAgreement::class.java)
}