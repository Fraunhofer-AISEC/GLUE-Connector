package de.fraunhofer.aisec.glue.pipeline

/**
 * The configuration object that is injected into every pipeline container at `/config.json`.
 */
data class ContainerConfig(
    /**
     * The address the controller can be reached on.
     */
    val controller: String,
    /**
     * The address or hostname the next container is reachable on or null if this is the last container in the pipeline.
     * For now, this is just the name of the container.
     *       Alternatively, we can also add a port to the config that we just expect the container to listen on.
     */
    val nextContainer: String?,
)
