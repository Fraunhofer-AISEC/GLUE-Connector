package de.fraunhofer.iosb.odrl

import de.fraunhofer.iosb.odrl.evaluator.Context
import de.fraunhofer.iosb.odrl.evaluator.OdrlPolicyEvaluator
import de.fraunhofer.iosb.odrl.evaluator.Result
import de.fraunhofer.iosb.odrl.glue.GlueContainerPipeline
import de.fraunhofer.iosb.odrl.glue.GlueIdentifiers
import de.fraunhofer.iosb.odrl.parser.OdrlParser
import kotlinx.coroutines.runBlocking
import java.io.BufferedReader
import kotlin.system.exitProcess

fun loadFileOrDie(fileName: String): BufferedReader {
    val reader = {}.javaClass.getResourceAsStream(fileName)?.bufferedReader()
    if (reader == null) {
        println("Failed to load file $fileName")
        exitProcess(-1)
    }
    return reader
}

fun extractPipelinesFromAction(action: OdrlAction): Pair<Result, List<GlueContainerPipeline>> {
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

fun onActionCallback(action: OdrlAction): Result {
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

fun main() {
    runBlocking {
        val p = OdrlParser()
        val e = OdrlPolicyEvaluator()

        var policy = p.readOdrlPolicy(loadFileOrDie("/policy-example-1.json"))
        var context = Context("http://www.w3.org/ns/odrl/2/play", "http://example.com/target")
        println("Evaluating $policy on $context")
        var decision = e.evaluate(context, policy)
        println("Decision of policy-example-1.json is $decision (should be: ALLOW)")

        policy = p.readOdrlPolicy(loadFileOrDie("/policy-example-2.json"))
        context = Context("http://www.w3.org/ns/odrl/2/distribute", "http://example.com/DataModel1")
        println("\nEvaluating $policy on $context")
        decision = e.evaluate(context, policy)
        println("Decision of policy-example-2.json is $decision (should be: DENY)")
        println("Detailed decision reasoning: ${decision.evaluationResult.failureReasoning}")

        policy = p.readOdrlPolicy(loadFileOrDie("/policy-example-3.json"))
        context = Context("http://www.w3.org/ns/odrl/2/use", "http://dataowner.com/document:1234")
        println("\nEvaluating $policy on $context")
        decision = e.evaluate(context, policy, { onActionCallback(it) })
        println("Decision of policy-example-3.json when USING asset is $decision (should be: ALLOW)")

        context = Context("http://www.w3.org/ns/odrl/2/distribute", "http://dataowner.com/document:1234")
        println("\nEvaluating $policy on $context")
        decision = e.evaluate(context, policy, { onActionCallback(it) })
        println("Decision of policy-example-3.json when DISTRIBUTING asset is $decision (should be: ALLOW)")

        // policy-example-4.json only works with glue-odrl.jsonld in the file system root (e.g. C:\)
        /*
        policy = p.readOdrlPolicy(loadFileOrDie("/policy-example-4.json"))
        println("\nEvaluating $policy on $context")
        decision = e.evaluate(context, policy, { onActionCallback(it) })
        println("Decision of policy-example-4.json when DISTRIBUTING asset is $decision (should be: ALLOW)")
         */
    }
}