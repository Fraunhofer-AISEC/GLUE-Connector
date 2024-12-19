package de.fraunhofer.aisec.glue.models.negotiation

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import de.fraunhofer.aisec.glue.jsonld.BoxedStringDeserializer
import de.fraunhofer.aisec.glue.jsonld.JsonLDType
import de.fraunhofer.iosb.odrl.OdrlOffer

@JsonLDType
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonTypeName("${Namespaces.DSPACE}/ContractRequestMessage")
data class ContractRequestMessage(
    @JsonProperty("${Namespaces.DSPACE}/providerPid")
    val providerPid: String? = null,
    @JsonProperty("${Namespaces.DSPACE}/consumerPid")
    val consumerPid: String,
    @JsonProperty("${Namespaces.DSPACE}/callbackAddress")
    @JsonDeserialize(using = BoxedStringDeserializer::class)
    val callbackAddress: String,
    @JsonProperty("${Namespaces.DSPACE}/offer")
    val offer: OdrlOffer,
)
