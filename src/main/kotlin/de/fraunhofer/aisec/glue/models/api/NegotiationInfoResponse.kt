package de.fraunhofer.aisec.glue.models.api

import de.fraunhofer.aisec.glue.models.negotiation.NegotiationState
import de.fraunhofer.aisec.glue.negotiation.NegotiationImpl

data class NegotiationInfoResponse(
    val role: NegotiationImpl.Role,
    val peer: String,
    val providerPid: String,
    val consumerPid: String,
    val state: NegotiationState,
)
