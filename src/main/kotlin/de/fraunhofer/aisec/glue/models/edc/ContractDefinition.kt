package de.fraunhofer.aisec.glue.models.edc

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import de.fraunhofer.iosb.odrl.parser.JsonLd
import java.net.URI

@JsonLd(Contexts.EDC)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonTypeName("${Namespaces.EDC}/ContractDefinition")
data class ContractDefinition(
    @JsonProperty("@id")
    val uid: URI,
    @JsonProperty("${Namespaces.EDC}/accessPolicyId")
    val accessPolicyId: URI,
    @JsonProperty("${Namespaces.EDC}/contractPolicyId")
    val contractPolicyId: URI,
    @JsonProperty("${Namespaces.EDC}/assetSelector")
    val assetSelector: List<String>,
    @JsonProperty("${Namespaces.EDC}/privateProperties")
    val privateProperties: Map<String, Any>,
)
