package de.fraunhofer.aisec.glue.pipeline

import fhg.glue.odrl.GlueParameter

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
     */
    val nextContainer: String?,

    /**
     * The values of all arguments from the policy as specified by its parameters.
     * Parameters will be identified by their full ODRL/JsonLD types with the namespace expanded.
     */
    val parameters: List<GlueParameter>

    // TODO: When used as a contract compiler pipeline, are more options needed?
    //       We probably don't need to include the parameters to expand since it should know that already
)
