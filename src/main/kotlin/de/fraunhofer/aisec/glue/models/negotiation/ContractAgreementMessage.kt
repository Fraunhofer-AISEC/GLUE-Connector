package de.fraunhofer.aisec.glue.models.negotiation

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.databind.JsonNode
import de.fraunhofer.aisec.glue.jsonld.JsonLDType
import de.fraunhofer.iosb.odrl.OdrlAgreement

@JsonLDType
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonTypeName("${Namespaces.DSPACE}/ContractOfferMessage")
data class ContractAgreementMessage(
    @JsonProperty("${Namespaces.DSPACE}/providerPid")
    val providerPid: String,
    @JsonProperty("${Namespaces.DSPACE}/consumerPid")
    val consumerPid: String,
    @JsonProperty("${Namespaces.DSPACE}/agreement")
    val agreement: OdrlAgreement,
    @JsonProperty("${Namespaces.DSPACE}/callbackAddress")
    val callbackAddress: String,
)
