package de.fraunhofer.aisec.glue.models.negotiation

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import de.fraunhofer.aisec.glue.jsonld.JsonLDType

@JsonLDType
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonTypeName("${Namespaces.DSPACE}/ContractNegotiationError")
data class ContractNegotiationError(
    @JsonProperty("${Namespaces.DSPACE}/providerPid")
    val providerPid: String,
    @JsonProperty("${Namespaces.DSPACE}/consumerPid")
    val consumerPid: String,
    @JsonProperty("${Namespaces.DSPACE}/code")
    val code: String,
    @JsonProperty("${Namespaces.DSPACE}/reason")
    val reason: List<Any>,
    @JsonProperty("${Namespaces.DC_TERMS}/description")
    val description: List<MultilanguageProperty>,
)
