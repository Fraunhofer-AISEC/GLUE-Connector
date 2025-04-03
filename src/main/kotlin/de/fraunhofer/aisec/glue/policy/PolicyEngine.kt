package de.fraunhofer.aisec.glue.policy

import de.fraunhofer.aisec.glue.pipeline.PipelineManager
import de.fraunhofer.iosb.odrl.OdrlPolicy
import de.fraunhofer.iosb.odrl.evaluator.Context
import de.fraunhofer.iosb.odrl.evaluator.OdrlPolicyEvaluator
import java.net.URI

class PolicyEngine(private val pipelineManager: PipelineManager) {
    /**
     * Evaluate an ODRL policy, executing all actions and
     */
    suspend fun evaluatePolicy(
        policy: OdrlPolicy,
        action: URI,
        target: URI,
    ): Result<PolicyEvaluationResult> {
        val context = Context(action, target)
        val evaluation = PolicyEvaluation()

        val decision =
            OdrlPolicyEvaluator()
                .evaluate(context, policy, evaluation::actionCallback, evaluation::infringementCallback)

        if (!decision.evaluationResult.success || decision.infringementViolations.isNotEmpty()) {
            val messageBuilder = StringBuilder("Use of the requested resource is prohibited:")
            if (!decision.evaluationResult.success) {
                messageBuilder.append("policy evaluation failed: ${decision.evaluationResult.failureReasoning}")
            }
            if (!decision.evaluationResult.success && decision.infringementViolations.isNotEmpty()) {
                messageBuilder.append(" and ")
            }
            if (decision.infringementViolations.isNotEmpty()) {
                val infringements = decision.infringementViolations.joinToString { a -> a.value.uid.toString() }
                messageBuilder.append("the following action(s) violate the policy's prohibitions: $infringements")
            }

            return Result.failure(PolicyViolationException(messageBuilder.toString()))
        }

        // Actualize the container pipeline, if any
        val pipelineResult = evaluation.containerPipeline?.runCatching {
            pipelineManager.createPipeline(this, policy.uid.toString(), target.toString())
        }

        return runCatching {
            PolicyEvaluationResult(
                pipelineResult?.getOrThrow(),
                evaluation.nextPolicyTemplate,
            )
        }
    }
}
