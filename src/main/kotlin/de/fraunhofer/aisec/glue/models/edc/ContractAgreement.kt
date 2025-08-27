package de.fraunhofer.aisec.glue.models.edc

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.databind.JsonNode
import de.fraunhofer.iosb.odrl.parser.JsonLd

@JsonLd(Contexts.EDC)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonTypeName("${Namespaces.EDC}/ContractAgreement")
data class ContractAgreement(
    @param:JsonProperty("@id")
    val id: String,
    @param:JsonProperty("${Namespaces.EDC}/providerId")
    val providerId: String,
    @param:JsonProperty("${Namespaces.EDC}/consumerId")
    val consumerId: String,
    @param:JsonProperty("${Namespaces.EDC}/contractSigningDate")
    val contractSigningDate: Long,
    @param:JsonProperty("${Namespaces.EDC}/assetId")
    val assetId: String,
    @param:JsonProperty("${Namespaces.EDC}/policy")
    val policy: JsonNode
)
