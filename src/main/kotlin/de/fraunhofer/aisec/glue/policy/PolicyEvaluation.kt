package de.fraunhofer.aisec.glue.policy

import de.fraunhofer.iosb.odrl.OdrlAction
import de.fraunhofer.iosb.odrl.OdrlConstraint
import de.fraunhofer.iosb.odrl.OdrlIdentifiers
import de.fraunhofer.iosb.odrl.evaluator.Result
import de.fraunhofer.iosb.odrl.glue.GlueContainerPipeline
import de.fraunhofer.iosb.odrl.glue.GlueIdentifiers

internal class PolicyEvaluation {
    var containerPipeline: GlueContainerPipeline? = null

    /**
     * This function evaluates the DeployPipeline action and extracts a runnable container pipeline from it
     * This action needs a refinement constraint defining the pipeline to run.
     * For now, only a single constraint specifying the exact pipeline is supported, but an OR constraint specifying
     * multiple pipelines may also be feasible.
     */
    private fun handlePipelineAction(action: OdrlAction): Result {
        // Extract the container pipeline definition and reject everything else
        val pipelineDefs = mutableListOf<GlueContainerPipeline>()
        for (refinement in action.refinement) {
            // From all logical constraints, only OR and maybe XONE can make sense.
            // More than one OR constraint matches could be handled like set operations.
            if (refinement !is OdrlConstraint) {
                return Result(false, "Logical constraints are not supported at this time")
            }
            // I don't think there is any other refinement we have to support
            if (refinement.leftOperand.uid.toString() != GlueIdentifiers.HAS_CONTAINER_PIPELINE) {
                return Result(false, "Constraint type ${refinement.leftOperand.uid} is not supported")
            }

            // We may want to support some set operations in the future
            if (refinement.operator.uid.toString() != OdrlIdentifiers.EQ) {
                return Result(false, "Unsupported operator ${refinement.operator.uid} for container pipeline constraint")
            }

            if (refinement.rightOperand.size != 1) {
                return Result(false, "The container pipeline constraint does not support set operations")
            }

            pipelineDefs += refinement.rightOperand[0] as? GlueContainerPipeline ?: return Result(
                false,
                "Invalid type ${refinement.rightOperand[0].javaClass.simpleName} for container pipeline",
            )
        }

        // For now, we only allow one pipeline definition.
        // In the future, there may be multiple options with some selection mechanism.
        // this policy stuff can get arbitrarily complex...
        if (pipelineDefs.size != 1) {
            return Result(false, "There must be exactly one pipeline definition for a spawn pipeline action")
        }

        if (containerPipeline != null) {
            return Result(false, "Only one container pipeline can be created at this time")
        }

        containerPipeline = pipelineDefs[0]
        return Result(true)
    }

    suspend fun actionCallback(action: OdrlAction): Result {
        return when (action.uid.toString()) {
            GlueIdentifiers.DEPLOY_PIPELINE -> handlePipelineAction(action)
            OdrlIdentifiers.NEXTPOLICY -> Result(false, "Not implemented yet")
            else -> Result(false, "Action ${action.uid} is not supported")
        }
    }

    suspend fun infringementCallback(action: OdrlAction): Boolean {
        return true
    }
}
