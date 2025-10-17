package de.fraunhofer.aisec.glue.models.edc

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import de.fraunhofer.iosb.odrl.OdrlPolicy
import de.fraunhofer.iosb.odrl.parser.JsonLd
import java.net.URI

@JsonLd(Contexts.EDC)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonTypeName("${Namespaces.EDC}/PolicyDefinition")
data class PolicyDefinition(
    @param:JsonProperty("@id")
    val uid: URI,
    @param:JsonProperty("${Namespaces.EDC}/policy")
    val policy: OdrlPolicy,
    @param:JsonProperty("${Namespaces.EDC}/privateProperties")
    @param:JsonInclude(JsonInclude.Include.NON_EMPTY)
    val privateProperties: Map<String, Any>? = null,
)
