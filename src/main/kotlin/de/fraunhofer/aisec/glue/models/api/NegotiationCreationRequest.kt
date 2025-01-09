package de.fraunhofer.aisec.glue.models.api

import de.fraunhofer.iosb.odrl.OdrlOffer

data class NegotiationCreationRequest(
    val peer: String,
    val contractRequest: OdrlOffer,
)
