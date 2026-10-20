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

import de.fraunhofer.iosb.odrl.OdrlLeftOperand
import de.fraunhofer.iosb.odrl.XmlDateTime
import de.fraunhofer.iosb.odrl.glue.GlueContainer
import de.fraunhofer.iosb.odrl.glue.GlueContainerPipeline
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OdrlLeftOperandEvaluatorTest {
    private val evaluator = OdrlLeftOperandEvaluator()

    @Test
    fun testLeftOperandDateTimeEvaluation() {
        val ctx = Context("http://example.com/action", "http://example.com/asset")
        val operand = evaluator.evaluate(ctx, OdrlLeftOperand("http://www.w3.org/ns/odrl/2/dateTime"))
        assertIs<XmlDateTime>(operand)
        assertTrue(operand.value.isBefore(LocalDateTime.now().plusMinutes(1)))
        assertTrue(operand.value.plusMinutes(1).isAfter(LocalDateTime.now()))
    }

    @Test
    fun testLeftOperandPipelineEvaluation() {
        val ctx =
            Context(
                "http://example.com/action",
                "http://example.com/asset",
                GlueContainerPipeline(
                    container =
                        listOf(
                            GlueContainer("test/container1", "364a4787748a98c1c28085d4154a06c3a9be9a67c0b08a56b3f69fa8fabf172e"),
                        ),
                ),
            )
        val operand = evaluator.evaluate(ctx, OdrlLeftOperand("http://fhg.de/glue/containerPipeline"))
        assertIs<GlueContainerPipeline>(operand)
        assertEquals(ctx.params["pipeline"], operand)
    }
}
