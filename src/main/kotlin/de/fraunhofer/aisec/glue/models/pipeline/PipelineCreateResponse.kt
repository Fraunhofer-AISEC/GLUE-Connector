package de.fraunhofer.aisec.glue.models.pipeline

data class PipelineCreateResponse(
    val pipelineName: String,
    val pipelinePort: Int,
)
