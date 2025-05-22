package de.fraunhofer.aisec.glue.models.edc

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import de.fraunhofer.iosb.odrl.parser.JsonLd
import java.net.URI

@JsonLd(Contexts.EDC)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonTypeName("${Namespaces.EDC}/TerminateNegotiation")
data class TerminateNegotiationRequest(
    @JsonProperty("@id")
    val uid: URI,
    @JsonProperty("${Namespaces.EDC}/reason")
    val reason: String,
)
