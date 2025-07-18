package de.fraunhofer.aisec.glue.models.api

import de.fraunhofer.aisec.glue.negotiation.Negotiation

data class NegotiationInfoResponse(
    val id: String,
    val role: Negotiation.Role,
    val peer: String,
    val state: String,
)
