package de.fraunhofer.aisec.glue.models.pipeline

data class PipelineCreateResponse(
    val pipelineName: String,
    // TODO: Convert this to a List<ContainerPort> and update examples/uapi
    val pipelinePort: Int,
)
