package de.fraunhofer.aisec.glue.models.pipeline

import de.fraunhofer.aisec.glue.pipeline.ContainerPort

data class PipelineCreateResponse(
    val pipelineName: String,
    val ports: List<ContainerPort>,
)
