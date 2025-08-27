package de.fraunhofer.aisec.glue.models.edc

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.databind.node.ObjectNode
import de.fraunhofer.iosb.odrl.parser.JsonLd

@JsonLd(Contexts.EDC)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonTypeName("${Namespaces.EDC}/ContractRequest")
data class ContractRequest(
    @param:JsonProperty("${Namespaces.EDC}/counterPartyAddress")
    val counterPartyAddress: String,
    @param:JsonProperty("${Namespaces.EDC}/protocol")
    val protocol: String,
    @param:JsonProperty("${Namespaces.EDC}/policy")
    val policy: ObjectNode,
    @param:JsonProperty("${Namespaces.EDC}/callbackAddresses")
    val callbackAddresses: List<CallbackAddress>? = null,
)
