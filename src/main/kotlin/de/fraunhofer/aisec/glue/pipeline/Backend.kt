package de.fraunhofer.aisec.glue.pipeline

import fhg.glue.odrl.GlueContainerPipeline

/**
 * This interface abstracts over all possible backends that a container pipeline may have.
 * Currently, only the docker backend exists, but other runtimes (i.e. k8s) may come later.
 */
interface Backend {
    /**
     * Create a new pipeline from the provided ODRL object
     */
    suspend fun createPipeline(
        id: String,
        policyObject: GlueContainerPipeline,
    ): ContainerPipeline

    /**
     * Get the IP address of the coordinator relative to a container.
     */
    suspend fun getConnectorIp(relativeTo: ContainerInfo): String
}
