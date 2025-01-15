package de.fraunhofer.iosb.odrl.evaluator

import de.fraunhofer.iosb.odrl.*
import de.fraunhofer.iosb.odrl.Commons.Companion.loadFileOrDie
import de.fraunhofer.iosb.odrl.glue.GlueContainerPipeline
import de.fraunhofer.iosb.odrl.glue.GlueIdentifiers
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OdrlPolicyEvaluatorTest {
    private val parser = Commons.parserInstance
    private val evaluator = OdrlPolicyEvaluator()

    private fun extractPipelinesFromAction(action: OdrlAction): Pair<Result, List<GlueContainerPipeline>> {
        if (action.refinement.size != 1)
            return Pair(Result(false, "Expected single refinement in $action"), listOf())
        val constraint = action.refinement[0]
        if (constraint !is OdrlConstraint)
            return Pair(Result(false, "Refinement of action ${action.uid} must be constraint"), listOf())
        if (!constraint.rightOperand.all { it is GlueContainerPipeline }) {
            return Pair(Result(
                false,
                "Refinement constraint $constraint may only have right operands of type GlueContainerPipeline"
            ), listOf())
        }
        return Pair(Result(true), constraint.rightOperand.map { it as GlueContainerPipeline })
    }

    private fun onActionCallback(action: OdrlAction): Result {
        when (action.uid.toString()) {
            GlueIdentifiers.DEPLOY_PIPELINE,
            OdrlIdentifiers.NEXTPOLICY -> {
                val pipelines = extractPipelinesFromAction(action)
                if (!pipelines.first.success)
                    return pipelines.first
                for (pipeline in pipelines.second) {
                    println("   | Launching pipeline $pipeline")
                }
                return Result(true)
            }
            else -> return Result(false, "Unhandled action $action")
        }
    }

    @Test
    fun testPolicyExample1() {
        runBlocking {
            val policy = parser.readOdrlPolicy(loadFileOrDie("/policy-example-1.json"))
            val context = Context("http://www.w3.org/ns/odrl/2/play", "http://example.com/target")
            println("Evaluating $policy on $context")
            val decision = evaluator.evaluate(context, policy)
            println("Decision of policy-example-1.json is $decision (should be: ALLOW)")
            assertTrue(decision.evaluationResult.success)
        }
    }

    @Test
    fun testPolicyExample2() {
        runBlocking {
            val policy = parser.readOdrlPolicy(loadFileOrDie("/policy-example-2.json"))
            val context = Context("http://www.w3.org/ns/odrl/2/distribute", "http://example.com/DataModel1")
            println("\nEvaluating $policy on $context")
            val decision = evaluator.evaluate(context, policy)
            println("Decision of policy-example-2.json is $decision (should be: DENY)")
            println("Detailed decision reasoning: ${decision.evaluationResult.failureReasoning}")
            assertFalse(decision.evaluationResult.success)
        }
    }

    @Test
    fun testPolicyExample3() {
        runBlocking {
            val policy = parser.readOdrlPolicy(loadFileOrDie("/policy-example-3.json"))
            val context = Context("http://www.w3.org/ns/odrl/2/use", "http://dataowner.com/document:1234")
            println("\nEvaluating $policy on $context")
            val decision = evaluator.evaluate(context, policy, { onActionCallback(it) })
            println("Decision of policy-example-3.json when USING asset is $decision (should be: ALLOW)")
            assertTrue(decision.evaluationResult.success)
        }
    }

    @Test
    @Disabled
    // policy-example-4.json only works with glue-odrl.jsonld in the file system root (e.g. C:\)
    fun testPolicyExample4() {
        runBlocking {
            val policy = parser.readOdrlPolicy(loadFileOrDie("/policy-example-4.json"))
            val context = Context("http://www.w3.org/ns/odrl/2/use", "http://dataowner.com/document:1234")
            println("\nEvaluating $policy on $context")
            val decision = evaluator.evaluate(context, policy, { onActionCallback(it) })
            println("Decision of policy-example-4.json when DISTRIBUTING asset is $decision (should be: ALLOW)")
        }
    }
}