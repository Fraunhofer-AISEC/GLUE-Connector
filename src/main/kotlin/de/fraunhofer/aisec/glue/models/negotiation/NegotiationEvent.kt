package de.fraunhofer.aisec.glue.models.negotiation

import com.fasterxml.jackson.annotation.JsonProperty

enum class NegotiationEvent {
    @JsonProperty("${Namespaces.DSPACE}/ACCEPTED")
    ACCEPTED,

    @JsonProperty("${Namespaces.DSPACE}/FINALIZED")
    FINALIZED,
}
