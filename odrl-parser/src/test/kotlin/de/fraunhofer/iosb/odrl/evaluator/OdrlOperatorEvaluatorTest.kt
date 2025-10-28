package de.fraunhofer.iosb.odrl.evaluator

import de.fraunhofer.iosb.odrl.OdrlLeftOperand
import de.fraunhofer.iosb.odrl.OdrlOperator
import de.fraunhofer.iosb.odrl.XmlDate
import de.fraunhofer.iosb.odrl.XmlDateTime
import de.fraunhofer.iosb.odrl.glue.GlueContainer
import de.fraunhofer.iosb.odrl.glue.GlueContainerPipeline
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OdrlOperatorEvaluatorTest {
    private val evaluator = OdrlOperatorEvaluator()
    private val eq = OdrlOperator("http://www.w3.org/ns/odrl/2/eq")
    private val neq = OdrlOperator("http://www.w3.org/ns/odrl/2/neq")
    private val lt = OdrlOperator("http://www.w3.org/ns/odrl/2/lt")
    private val lteq = OdrlOperator("http://www.w3.org/ns/odrl/2/lteq")
    private val gteq = OdrlOperator("http://www.w3.org/ns/odrl/2/gteq")

    @Test
    fun testOperatorEvaluationWithRightDateTimeOperand() {
        val ctx = Context("http://example.com/action", "http://example.com/asset")
        val leftOperand = OdrlLeftOperand("http://www.w3.org/ns/odrl/2/dateTime")

        assertTrue(
            evaluator.evaluateWithRightOperand(
                ctx, gteq, leftOperand, listOf(XmlDateTime(LocalDateTime.now()))
            ).success
        )
        assertTrue(
            evaluator.evaluateWithRightOperand(
                ctx, lteq, leftOperand, listOf(XmlDateTime(LocalDateTime.now().plusMinutes(1)))
            ).success
        )
        assertFalse(
            evaluator.evaluateWithRightOperand(
                ctx, lt, leftOperand, listOf(XmlDate(LocalDate.of(2000, 1, 1)))
            ).success
        )
    }

    @Test
    fun testOperatorEvaluationWithRightPipelineOperand() {
        val ctx = Context(
            "http://example.com/action", "http://example.com/asset",
            GlueContainerPipeline(
                container = listOf(
                    GlueContainer("test/container1", "364a4787748a98c1c28085d4154a06c3a9be9a67c0b08a56b3f69fa8fabf172e")
                )
            )
        )
        val leftOperand = OdrlLeftOperand("http://fhg.de/glue/containerPipeline")

        assertTrue(
            evaluator.evaluateWithRightOperand(
                ctx, eq, leftOperand, listOf(ctx.params["pipeline"] as GlueContainerPipeline)
            ).success
        )
        assertTrue(
            evaluator.evaluateWithRightOperand(
                ctx, neq, leftOperand, listOf(
                    GlueContainerPipeline(
                        container = listOf(
                            GlueContainer(
                                image = "testContainer",
                                hash = "364a4787748a98c1c28085d4154a06c3a9be9a67c0b08a56b3f69fa8fabf172e"
                            )
                        )
                    )
                )
            ).success
        )
    }
}