package de.fraunhofer.aisec.glue.pipeline

data class ContainerPipelineInfo(
    val name: String,
    val contractId: String,
    val targetId: String,
    val backend: String,
    val containers: List<ContainerInfo>,
) {
    companion object {
        suspend fun from(pipeline: ContainerPipeline): ContainerPipelineInfo {
            val backend =
                pipeline::class.simpleName
                    ?.removeSuffix("ContainerPipeline")
                    ?: "Unknown"

            return ContainerPipelineInfo(
                name = pipeline.name,
                contractId = pipeline.contractId,
                targetId = pipeline.targetId,
                backend = backend,
                containers = pipeline.getContainerInfo(),
            )
        }
    }
}
