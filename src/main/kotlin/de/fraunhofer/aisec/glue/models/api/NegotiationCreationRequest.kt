package de.fraunhofer.aisec.glue.models.api

import de.fraunhofer.iosb.odrl.OdrlPolicy
import de.fraunhofer.iosb.odrl.glue.GlueContainerPipeline

data class NegotiationCreationRequest(
    val peer: String,
    val contractRequest: OdrlPolicy,
    //  an API namespace and some such)
    val localPipeline: GlueContainerPipeline? = null,
)
