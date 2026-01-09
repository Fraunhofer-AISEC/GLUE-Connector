package de.fraunhofer.aisec.glue.models.api

import de.fraunhofer.iosb.odrl.OdrlPolicy
import de.fraunhofer.iosb.odrl.glue.GlueContainerPipeline

data class NegotiationCreationRequest(
    val peer: String,
    val contractRequest: OdrlPolicy,
    // TODO: This type has JsonLD annotations, which we don't really want or need in the API I think (unless we define
    //  an API namespace and some such)
    val localPipeline: GlueContainerPipeline? = null,
)
