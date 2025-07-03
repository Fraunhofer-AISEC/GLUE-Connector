package de.fraunhofer.aisec.glue.models.edc

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import de.fraunhofer.iosb.odrl.parser.JsonLd

@JsonLd(Contexts.EDC)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonTypeName("${Namespaces.EDC}/CallbackAddress")
data class CallbackAddress(
    @JsonProperty("${Namespaces.EDC}/authCodeId")
    val authCodeId: String?,
    @JsonProperty("${Namespaces.EDC}/authKey")
    val authKey: String?,
    @JsonProperty("${Namespaces.EDC}/events")
    val events: Set<String>,
    @JsonProperty("${Namespaces.EDC}/transactional")
    val transactional: Boolean,
    @JsonProperty("${Namespaces.EDC}/uri")
    val uri: String,
)