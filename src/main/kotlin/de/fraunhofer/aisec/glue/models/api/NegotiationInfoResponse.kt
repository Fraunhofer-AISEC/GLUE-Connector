package de.fraunhofer.aisec.glue.models.api

import de.fraunhofer.aisec.glue.models.negotiation.NegotiationState
import de.fraunhofer.aisec.glue.negotiation.Negotiation

data class NegotiationInfoResponse(
    val role: Negotiation.Role,
    val peer: String,
    val providerPid: String,
    val consumerPid: String,
    val state: NegotiationState,
)