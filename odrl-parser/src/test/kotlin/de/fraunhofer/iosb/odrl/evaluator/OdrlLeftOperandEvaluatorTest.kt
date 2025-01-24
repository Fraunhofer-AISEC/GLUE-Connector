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
        val ctx = Context(
            "http://example.com/action", "http://example.com/asset", GlueContainerPipeline(
                container = listOf(
                    GlueContainer("test/container1", "364a4787748a98c1c28085d4154a06c3a9be9a67c0b08a56b3f69fa8fabf172e")
                )
            )
        )
        val operand = evaluator.evaluate(ctx, OdrlLeftOperand("http://fhg.de/glue/containerPipeline"))
        assertIs<GlueContainerPipeline>(operand)
        assertEquals(ctx.params["pipeline"], operand)
    }
}