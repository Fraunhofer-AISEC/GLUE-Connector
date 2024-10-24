package de.fraunhofer.aisec.glue.models.api

import com.fasterxml.jackson.databind.JsonNode

data class NegotiationCreationRequest(
    val peer: String,
    val contractRequest: JsonNode,
)
