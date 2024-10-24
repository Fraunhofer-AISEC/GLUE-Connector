package de.fraunhofer.aisec.glue.models.api

import de.fraunhofer.aisec.glue.models.pipeline.PipelineCreateResponse

data class RemotePipelineStatusResponse(
    /** The name of the peer connector this pipeline is running on */
    val peer: String,
    /** The local ID of the remote pipeline */
    val localId: String,
    /** The status message returned by the remote connector */
    val remote: PipelineCreateResponse
)