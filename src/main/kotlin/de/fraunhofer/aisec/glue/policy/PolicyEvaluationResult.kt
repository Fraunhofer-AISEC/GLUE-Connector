package de.fraunhofer.aisec.glue.policy

import com.fasterxml.jackson.databind.JsonNode
import de.fraunhofer.aisec.glue.pipeline.ContainerPipeline

data class PolicyEvaluationResult(
    val containerPipeline: ContainerPipeline?,
    val nextPolicyTemplate: JsonNode?,
)
