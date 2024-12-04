package de.fraunhofer.aisec.glue.pipeline

import java.io.Closeable

abstract class ContainerPipeline(val name: String) : Closeable {
    abstract suspend fun start()

    abstract suspend fun stop()

    abstract suspend fun remove()

    /**
     * Get up-to-date info on the container at the specified index.
     */
    abstract suspend fun getContainerInfo(index: Int): ContainerInfo?

    /**
     * Get up-to-date info on all containers.
     */
    abstract suspend fun getContainerInfo(): List<ContainerInfo>

    /**
     * Inject a configuration file into the container.
     */
    abstract suspend fun injectConfig(index: Int, config: ContainerConfig)
}
