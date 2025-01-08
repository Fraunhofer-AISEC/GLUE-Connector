package de.fraunhofer.aisec.glue.models.negotiation

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import de.fraunhofer.iosb.odrl.parser.JsonLd
import de.fraunhofer.iosb.odrl.OdrlOffer

@JsonLd(Contexts.DSPACE)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonTypeName("${Namespaces.DSPACE}/ContractOfferMessage")
data class ContractOfferMessage(
    @JsonProperty("${Namespaces.DSPACE}/providerPid")
    val providerPid: String,
    @JsonProperty("${Namespaces.DSPACE}/consumerPid")
    val consumerPid: String? = null,
    @JsonProperty("${Namespaces.DSPACE}/callbackAddress")
    val callbackAddress: String,
    @JsonProperty("${Namespaces.DSPACE}/offer")
    val offer: OdrlOffer,
)
