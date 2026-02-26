package de.fraunhofer.aisec.glue.models.negotiation

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import de.fraunhofer.iosb.odrl.parser.JsonLd

@JsonLd(Contexts.DSPACE)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonTypeName("${Namespaces.DSPACE}/ContractNegotiationEventMessage")
data class ContractNegotiationEventMessage(
    @param:JsonProperty("${Namespaces.DSPACE}/providerPid")
    val providerPid: String,
    @param:JsonProperty("${Namespaces.DSPACE}/consumerPid")
    val consumerPid: String,
    @param:JsonProperty("${Namespaces.DSPACE}/eventType")
    val eventType: NegotiationEvent,
): DspMessage
