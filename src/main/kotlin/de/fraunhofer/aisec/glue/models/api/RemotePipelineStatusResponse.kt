package de.fraunhofer.aisec.glue.models.api

import com.fasterxml.jackson.annotation.JsonUnwrapped
import de.fraunhofer.aisec.glue.models.pipeline.PipelineCreateResponse
import de.fraunhofer.aisec.glue.pipeline.ContainerPort

data class RemotePipelineStatusResponse(
    /** The name of the peer connector this pipeline is running on */
    val peer: String,
    /** The local ID of the remote pipeline */
    val localId: String,
    val remoteId: String,
    val ports: List<ContainerPort>,
)
