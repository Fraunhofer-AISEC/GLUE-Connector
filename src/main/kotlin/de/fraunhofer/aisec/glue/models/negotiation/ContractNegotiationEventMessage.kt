package de.fraunhofer.aisec.glue.models.negotiation

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import de.fraunhofer.aisec.glue.jsonld.JsonLDType

@JsonLDType
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonTypeName("${Namespaces.DSPACE}/ContractNegotiationEventMessage")
data class ContractNegotiationEventMessage(
    @JsonProperty("${Namespaces.DSPACE}/providerPid")
    val providerPid: String,
    @JsonProperty("${Namespaces.DSPACE}/consumerPid")
    val consumerPid: String,
    @JsonProperty("${Namespaces.DSPACE}/eventType")
    val eventType: NegotiationEvent,
)
