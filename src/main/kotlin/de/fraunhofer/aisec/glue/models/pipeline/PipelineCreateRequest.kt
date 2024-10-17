package de.fraunhofer.aisec.glue.models.pipeline

data class PipelineCreateRequest(
    // The target more or less identifies the pipeline to use instead of a data asset
    val target: String,
    // The contract id must be specified if there are more contracts referring to the target
    val contractId: String? = null,
    // If there is more than one action in the selected contract, an action must be specified as well
    val action: String? = null,
)
