package de.fraunhofer.aisec.glue.models.negotiation

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import de.fraunhofer.aisec.glue.jsonld.BoxedStringDeserializer
import de.fraunhofer.iosb.odrl.OdrlOffer
import de.fraunhofer.iosb.odrl.parser.JsonLd

@JsonLd(Contexts.DSPACE)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonTypeName("${Namespaces.DSPACE}/ContractRequestMessage")
data class ContractRequestMessage(
    @param:JsonProperty("@id")
    val uid: String,
    @param:JsonProperty("${Namespaces.DSPACE}/providerPid")
    val providerPid: String? = null,
    @param:JsonProperty("${Namespaces.DSPACE}/consumerPid")
    val consumerPid: String,
    @param:JsonProperty("${Namespaces.DSPACE}/callbackAddress")
    @param:JsonDeserialize(using = BoxedStringDeserializer::class)
    val callbackAddress: String,
    @param:JsonProperty("${Namespaces.DSPACE}/offer")
    val offer: OdrlOffer,
): DspMessage
