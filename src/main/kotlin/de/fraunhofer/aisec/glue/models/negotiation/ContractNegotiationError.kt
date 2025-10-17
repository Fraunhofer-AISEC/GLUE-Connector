package de.fraunhofer.aisec.glue.models.negotiation

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import de.fraunhofer.iosb.odrl.parser.JsonLd

@JsonLd(Contexts.DSPACE)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonTypeName("${Namespaces.DSPACE}/ContractNegotiationError")
data class ContractNegotiationError(
    @param:JsonProperty("${Namespaces.DSPACE}/providerPid")
    val providerPid: String,
    @param:JsonProperty("${Namespaces.DSPACE}/consumerPid")
    val consumerPid: String,
    @param:JsonProperty("${Namespaces.DSPACE}/code")
    val code: String,
    @param:JsonProperty("${Namespaces.DSPACE}/reason")
    val reason: List<Any>? = null,
    @param:JsonProperty("${Namespaces.DC_TERMS}/description")
    val description: List<MultilanguageProperty>? = null,
)
