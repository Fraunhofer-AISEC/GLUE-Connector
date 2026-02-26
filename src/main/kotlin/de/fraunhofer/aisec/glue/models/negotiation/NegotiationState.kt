package de.fraunhofer.aisec.glue.models.negotiation

import com.fasterxml.jackson.annotation.JsonProperty

enum class NegotiationState {
    /**
     * Note: The INITIAL state may never be sent to a peer
     */
    INITIAL,

    @JsonProperty("${Namespaces.DSPACE}/REQUESTED")
    REQUESTED,

    @JsonProperty("${Namespaces.DSPACE}/OFFERED")
    OFFERED,

    @JsonProperty("${Namespaces.DSPACE}/ACCEPTED")
    ACCEPTED,

    @JsonProperty("${Namespaces.DSPACE}/AGREED")
    AGREED,

    @JsonProperty("${Namespaces.DSPACE}/VERIFIED")
    VERIFIED,

    @JsonProperty("${Namespaces.DSPACE}/FINALIZED")
    FINALIZED,

    @JsonProperty("${Namespaces.DSPACE}/TERMINATED")
    TERMINATED,
}
