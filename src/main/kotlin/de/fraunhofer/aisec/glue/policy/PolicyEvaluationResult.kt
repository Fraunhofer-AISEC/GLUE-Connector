package de.fraunhofer.aisec.glue.policy

import de.fraunhofer.aisec.glue.pipeline.ContainerPipeline
import de.fraunhofer.iosb.odrl.glue.GlueValue

data class PolicyEvaluationResult(
    val containerPipeline: ContainerPipeline?,
    val nextPolicyTemplate: GlueValue?,
)
