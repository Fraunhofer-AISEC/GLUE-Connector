package de.fraunhofer.aisec.glue

import de.fraunhofer.aisec.glue.models.Contract

class ContainerManager(private val pdp: PDP) {
    private val pipelines = mutableMapOf<Contract, ContainerPipeline>()

    suspend fun pipelineForContract(contract: Contract): ContainerPipeline? {
        if (pipelines.contains(contract)) {
            return pipelines[contract]
        }

        // The pipeline does currently not exist, so we try to create it
        if (!pdp.checkStartPipeline(contract)) {
            return null
        }

        val containers =
            contract.pipeline.containers.map {
                Pair(it.name, createContainerCreateReq(image = it.image, cmd = it.cmd))
            }
        val pipeline = ContainerPipeline.create(containers)
        pipelines[contract] = pipeline
        return pipeline
    }
}
