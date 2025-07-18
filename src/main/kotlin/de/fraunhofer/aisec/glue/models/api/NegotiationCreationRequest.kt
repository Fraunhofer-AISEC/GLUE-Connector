package de.fraunhofer.aisec.glue.models.api

import de.fraunhofer.iosb.odrl.OdrlPolicy

data class NegotiationCreationRequest(
    val peer: String,
    val contractRequest: OdrlPolicy,
)
