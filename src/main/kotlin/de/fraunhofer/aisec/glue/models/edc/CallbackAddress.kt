package de.fraunhofer.aisec.glue.models.edc

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import de.fraunhofer.iosb.odrl.parser.JsonLd

@JsonLd(Contexts.EDC)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonTypeName("${Namespaces.EDC}/CallbackAddress")
data class CallbackAddress(
    @param:JsonProperty("${Namespaces.EDC}/authCodeId")
    val authCodeId: String?,
    @param:JsonProperty("${Namespaces.EDC}/authKey")
    val authKey: String?,
    @param:JsonProperty("${Namespaces.EDC}/events")
    val events: Set<String>,
    @param:JsonProperty("${Namespaces.EDC}/transactional")
    val transactional: Boolean,
    @param:JsonProperty("${Namespaces.EDC}/uri")
    val uri: String,
)
