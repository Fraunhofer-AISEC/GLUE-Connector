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
    @JsonProperty("@id")
    val id: String,
    @JsonProperty("${Namespaces.EDC}/providerId")
    val providerId: String,
    @JsonProperty("${Namespaces.EDC}/consumerId")
    val consumerId: String,
    @JsonProperty("${Namespaces.EDC}/contractSigningDate")
    val contractSigningDate: Long,
    @JsonProperty("${Namespaces.EDC}/assetId")
    val assetId: String,
    @JsonProperty("${Namespaces.EDC}/policy")
    val policy: JsonNode
)
