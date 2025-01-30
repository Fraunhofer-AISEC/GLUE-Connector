package de.fraunhofer.aisec.glue.policy

import de.fraunhofer.iosb.odrl.OdrlAction
import de.fraunhofer.iosb.odrl.OdrlConstraint
import de.fraunhofer.iosb.odrl.OdrlIdentifiers
import de.fraunhofer.iosb.odrl.evaluator.Result
import de.fraunhofer.iosb.odrl.glue.GlueContainerPipeline
import de.fraunhofer.iosb.odrl.glue.GlueIdentifiers
import de.fraunhofer.iosb.odrl.glue.GlueValue

internal class PolicyEvaluation {
    var containerPipeline: GlueContainerPipeline? = null
    var nextPolicyTemplate: GlueValue? = null

    /**
     * This function evaluates the DeployPipeline action and extracts a runnable container pipeline from it
     * This action needs a refinement constraint defining the pipeline to run.
     * For now, only a single constraint specifying the exact pipeline is supported, but an OR constraint specifying
     * multiple pipelines may also be feasible.
     * TODO: Extend this function to handle more complex pipeline definitions
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

        // HACK: This is not the best way to extract the pipeline def, but oh well...
        if (containerPipeline != null) {
            return Result(false, "Only one container pipeline can be created at this time")
        }

        // Return this as tuple to also support a pipeline for next contract
        containerPipeline = pipelineDefs[0]
        return Result(true)
    }

    private suspend fun handleNextPolicyAction(action: OdrlAction): Result {
        val policyTemplates = mutableListOf<GlueValue>()
        for (refinement in action.refinement) {
            if (refinement !is OdrlConstraint) {
                return Result(false, "Logical constraints are currently not supported")
            }

            // TODO: Also support policy compilers, refactor out left operand handling into different functions
            if (refinement.leftOperand.uid.toString() != GlueIdentifiers.HAS_POLICY_TEMPLATE) {
                return Result(false, "Unsupported refinement type for next policy: ${refinement.leftOperand.uid}")
            }

            if (refinement.operator.uid.toString() != OdrlIdentifiers.EQ) {
                return Result(false, "Invalid operator for policy template constraint: ${refinement.operator.uid}")
            }

            if (refinement.rightOperand.size != 1) {
                return Result(false, "Invalid number of right operands for policy template constraint")
            }

            policyTemplates += refinement.rightOperand[0] as? GlueValue ?:
                return Result(false, "Right operand for policy template must be of type glue:Value")
        }

        if (policyTemplates.size != 1) {
            return Result(false, "Next policy action must declare exactly one policy template")
        }

        nextPolicyTemplate = policyTemplates[0]
        return Result(true)
    }

    suspend fun actionCallback(action: OdrlAction): Result {
        return when (action.uid.toString()) {
            GlueIdentifiers.DEPLOY_PIPELINE -> handlePipelineAction(action)
            OdrlIdentifiers.NEXTPOLICY -> handleNextPolicyAction(action)
            else -> Result(false, "Action ${action.uid} is not supported")
        }
    }

    suspend fun infringementCallback(action: OdrlAction): Boolean {
        // TODO: Support infringement callback
        return true
    }
}
