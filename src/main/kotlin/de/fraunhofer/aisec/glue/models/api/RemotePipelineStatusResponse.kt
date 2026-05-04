package de.fraunhofer.aisec.glue.models.api

import de.fraunhofer.aisec.glue.pipeline.ContainerPort

data class RemotePipelineStatusResponse(
    /** The name of the peer connector this pipeline is running on */
    val peer: String,
    /** The local ID of the remote pipeline */
    val id: String,
    val remoteId: String,
    val ports: List<ContainerPort>,
)
