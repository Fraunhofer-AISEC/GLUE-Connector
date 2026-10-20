/*
 * Copyright 2025 Fraunhofer AISEC and Fraunhofer IOSB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.fraunhofer.iosb.odrl.evaluator

import com.fasterxml.jackson.databind.JsonNode
import de.fraunhofer.iosb.odrl.*
import de.fraunhofer.iosb.odrl.Commons.Companion.loadFileOrDie
import de.fraunhofer.iosb.odrl.glue.GlueContainerPipeline
import de.fraunhofer.iosb.odrl.glue.GlueIdentifiers
import de.fraunhofer.iosb.odrl.glue.GlueValue
import de.fraunhofer.iosb.odrl.glue.GlueValueProcessor
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OdrlPolicyEvaluatorTest {
    private val parser = Commons.parserInstance
    private val evaluator = OdrlPolicyEvaluator()

    private fun extractGluePipelinesOrThrow(action: OdrlAction): List<GlueContainerPipeline> {
        if (action.refinement.size != 1) {
            throw OdrlException("Expected a single refinement in $action")
        }
        val constraint = action.refinement[0]
        if (constraint !is OdrlConstraint) {
            throw OdrlException("Expected refinement of type constraint in $action")
        }
        if (constraint.leftOperand.uid.toString() != GlueIdentifiers.HAS_CONTAINER_PIPELINE) {
            throw OdrlException("Expected left operand ${GlueIdentifiers.HAS_CONTAINER_PIPELINE} in $action")
        }
        if (constraint.operator.uid.toString() != OdrlIdentifiers.EQ) {
            throw OdrlException("Expected operator ${OdrlIdentifiers.EQ} in $action")
        }
        if (!constraint.rightOperand.all { it is GlueContainerPipeline }) {
            throw OdrlException("Expected all right operands to be of type GlueContainerPipeline in $action")
        }
        return constraint.rightOperand.map { it as GlueContainerPipeline }
    }

    private fun extractPolicyTemplateOrThrow(action: OdrlAction): GlueValue {
        if (action.refinement.size != 1) {
            throw OdrlException("Expected a single refinement in $action")
        }
        val constraint = action.refinement[0]
        if (constraint !is OdrlConstraint) {
            throw OdrlException("Expected refinement of type constraint in $action")
        }
        if (constraint.leftOperand.uid.toString() != GlueIdentifiers.HAS_POLICY_TEMPLATE) {
            throw OdrlException("Expected left operand ${GlueIdentifiers.HAS_POLICY_TEMPLATE} in $action")
        }
        if (constraint.operator.uid.toString() != OdrlIdentifiers.EQ) {
            throw OdrlException("Expected operator ${OdrlIdentifiers.EQ} in $action")
        }
        if (constraint.rightOperand.size != 1) {
            throw OdrlException("Expected a single right operand in $action")
        }
        val operand = constraint.rightOperand[0]
        if (operand !is GlueValue) {
            throw OdrlException("Expected right operand to be of type GlueValue in $action")
        }
        return operand
    }

    private fun onParameterPlaceholderCallback(key: String): JsonNode =
        when (key) {
            "nextDataReceiverUid" -> {
                parser.mapper.convertValue("http://nextdatareceiver.com", JsonNode::class.java)
            }

            "nextDataReceiverConstraint" -> {
                parser.mapper.convertValue(
                    OdrlConstraint(
                        leftOperand = OdrlLeftOperand(OdrlIdentifiers.DATETIME),
                        operator = OdrlOperator(OdrlIdentifiers.LTEQ),
                        rightOperand =
                            listOf(
                                XmlDate(
                                    LocalDate.of(2035, 12, 31),
                                ),
                            ),
                    ),
                    JsonNode::class.java,
                )
            }

            else -> {
                throw OdrlException("Unsupported GlueParameterPlaceholder: $key")
            }
        }

    private suspend fun onActionCallback(action: OdrlAction): Result {
        when (action.value.uid.toString()) {
            GlueIdentifiers.DEPLOY_PIPELINE -> {
                try {
                    for (pipeline in extractGluePipelinesOrThrow(action)) {
                        println("   | Launching pipeline $pipeline")
                    }
                    return Result(true)
                } catch (e: OdrlException) {
                    return Result(false, e.message!!)
                }
            }

            OdrlIdentifiers.NEXTPOLICY -> {
                try {
                    for (pipeline in extractGluePipelinesOrThrow(action)) {
                        println("   | Launching policy compiler pipeline $pipeline")
                    }
                    return Result(true)
                } catch (_: OdrlException) {
                    // ignore
                }

                try {
                    val policyTemplate = extractPolicyTemplateOrThrow(action)
                    val policyJson =
                        GlueValueProcessor().resolvePlaceholders(policyTemplate) { onParameterPlaceholderCallback(it) }
                    val policy = parser.mapper.convertValue(policyJson.json, OdrlPolicy::class.java)
                    println("   | Deploying processed policy template $policy")
                    return Result(true)
                } catch (e: OdrlException) {
                    return Result(false, e.message!!)
                }
            }

            else -> {
                return Result(false, "Unhandled action $action")
            }
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

            var context = Context("http://www.w3.org/ns/odrl/2/use", "http://dataowner.com/document:1234")
            println("\nEvaluating $policy on $context")
            var decision = evaluator.evaluate(context, policy, { onActionCallback(it) })
            println("Decision of policy-example-3.json when USING asset is $decision (should be: ALLOW)")
            assertTrue(decision.evaluationResult.success)

            context = Context("http://www.w3.org/ns/odrl/2/distribute", "http://dataowner.com/document:1234")
            println("\nEvaluating $policy on $context")
            decision = evaluator.evaluate(context, policy, { onActionCallback(it) })
            println("Decision of policy-example-3.json when DISTRIBUTING asset is $decision (should be: ALLOW)")
            assertTrue(decision.evaluationResult.success)
        }
    }

    // policy-example-4.json only works with glue-odrl.jsonld in the file system root (e.g. C:\)
    @Test
    @Disabled
    fun testPolicyExample4() {
        runBlocking {
            val policy = parser.readOdrlPolicy(loadFileOrDie("/policy-example-4.json"))
            val context = Context("http://www.w3.org/ns/odrl/2/distribute", "http://dataowner.com/document:1234")
            println("\nEvaluating $policy on $context")
            val decision = evaluator.evaluate(context, policy, { onActionCallback(it) })
            println("Decision of policy-example-4.json when DISTRIBUTING asset is $decision (should be: ALLOW)")
            assertTrue(decision.evaluationResult.success)
        }
    }
}
