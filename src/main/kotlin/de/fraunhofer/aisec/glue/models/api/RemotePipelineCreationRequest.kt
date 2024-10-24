package de.fraunhofer.aisec.glue.models.api

import de.fraunhofer.aisec.glue.models.pipeline.PipelineCreateRequest

data class RemotePipelineCreationRequest(
    val peer: String,
    val request: PipelineCreateRequest,
)
