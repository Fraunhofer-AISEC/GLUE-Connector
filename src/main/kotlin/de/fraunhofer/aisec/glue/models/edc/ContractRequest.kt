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
    @JsonProperty("${Namespaces.EDC}/counterPartyAddress")
    val counterPartyAddress: String,
    @JsonProperty("${Namespaces.EDC}/protocol")
    val protocol: String,
    @JsonProperty("${Namespaces.EDC}/policy")
    val policy: ObjectNode,
    @JsonProperty("${Namespaces.EDC}/callbackAddresses")
    val callbackAddresses: List<String>? = null,
)
