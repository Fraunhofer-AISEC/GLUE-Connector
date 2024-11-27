package de.fraunhofer.aisec.glue.pipeline

import java.io.Closeable

abstract class ContainerPipeline(val name: String) : Closeable {
    abstract suspend fun start()

    abstract suspend fun stop()

    abstract suspend fun remove()

    // TODO: Split this up into info and status and cache info
    /**
     * Get up-to-date info on the container at the specified index.
     */
    abstract suspend fun getContainerInfo(index: Int): ContainerInfo?

    /**
     * Get up-to-date info on all containers.
     */
    abstract suspend fun getContainerInfo(): List<ContainerInfo>

    // TODO: Either provide extra information to inject or integrate this into the create function.
    //       As-is, the values for the config are backend-specific, so this provides little value.

    /**
     * Inject a configuration file into the container.
     */
    abstract suspend fun injectConfig(index: Int, config: ContainerConfig)
}
