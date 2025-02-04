package de.fraunhofer.aisec.glue.pipeline

import de.fraunhofer.iosb.odrl.glue.GlueContainerPipeline

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
        contractId: String,
        targetId: String,
    ): ContainerPipeline

    /**
     * Get the IP address of the coordinator relative to a container.
     */
    suspend fun getConnectorIp(relativeTo: ContainerInfo): String

    /**
     * Get the pipeline ID from an IP address belonging to a container.
     * This operation return null in cases where the IP does not belong to any pipeline
     */
    suspend fun getPipelineFromIp(ipAddress: String): String?
}
