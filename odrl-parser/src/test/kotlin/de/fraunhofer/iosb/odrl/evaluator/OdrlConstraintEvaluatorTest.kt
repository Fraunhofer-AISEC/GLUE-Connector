package de.fraunhofer.iosb.odrl.evaluator

import de.fraunhofer.iosb.odrl.*
import de.fraunhofer.iosb.odrl.glue.GlueContainer
import de.fraunhofer.iosb.odrl.glue.GlueContainerPipeline
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OdrlConstraintEvaluatorTest {
    private val evaluator = OdrlConstraintEvaluator()
    private val ctx = Context(
        "http://example.com/action",
        "http://example.com/asset",
        GlueContainerPipeline(
            listOf(
                GlueContainer(
                    image = "testContainer",
                    hash = "364a4787748a98c1c28085d4154a06c3a9be9a67c0b08a56b3f69fa8fabf172e"
                )
            )
        )
    )
    private val satisfiedConstraint1 =
        OdrlConstraint(
            leftOperand = OdrlLeftOperand("http://www.w3.org/ns/odrl/2/dateTime"),
            operator = OdrlOperator("http://www.w3.org/ns/odrl/2/gt"),
            rightOperand = listOf(XmlDate(LocalDate.of(2000, 1, 1)))
        )
    private val satisfiedConstraint2 =
        OdrlConstraint(
            leftOperand = OdrlLeftOperand("http://fhg.de/glue/containerPipeline"),
            operator = OdrlOperator("http://www.w3.org/ns/odrl/2/eq"),
            rightOperand = listOf(
                GlueContainerPipeline(
                    listOf(
                        GlueContainer(
                            image = "testContainer",
                            hash = "364a4787748a98c1c28085d4154a06c3a9be9a67c0b08a56b3f69fa8fabf172e"
                        )
                    )
                )
            )
        )
    private val unsatisfiedConstraint1 =
        OdrlConstraint(
            leftOperand = OdrlLeftOperand("http://www.w3.org/ns/odrl/2/dateTime"),
            operator = OdrlOperator("http://www.w3.org/ns/odrl/2/lt"),
            rightOperand = listOf(XmlDateTime(LocalDateTime.now().minusHours(1)))
        )
    private val unsatisfiedConstraint2 =
        OdrlConstraint(
            leftOperand = OdrlLeftOperand("http://fhg.de/glue/containerPipeline"),
            operator = OdrlOperator("http://www.w3.org/ns/odrl/2/neq"),
            rightOperand = listOf(
                GlueContainerPipeline(
                    listOf(
                        GlueContainer(
                            image = "testContainer",
                            hash = "364a4787748a98c1c28085d4154a06c3a9be9a67c0b08a56b3f69fa8fabf172e"
                        )
                    )
                )
            )
        )


    @Test
    fun testConstraintEvaluation() {
        assertTrue(
            evaluator.isSatisfied(ctx, satisfiedConstraint1).success
        )
    }

    @Test
    fun testLogicalConstraintEvaluationOr() {
        assertTrue(
            evaluator.isSatisfied(
                ctx,
                OdrlLogicalConstraint(
                    or = listOf(unsatisfiedConstraint1, unsatisfiedConstraint2, satisfiedConstraint1)
                )
            ).success
        )
        assertTrue(
            evaluator.isSatisfied(
                ctx,
                OdrlLogicalConstraint(
                    or = listOf(satisfiedConstraint1, unsatisfiedConstraint1, satisfiedConstraint2)
                )
            ).success
        )
        assertFalse(
            evaluator.isSatisfied(
                ctx,
                OdrlLogicalConstraint(
                    or = listOf(unsatisfiedConstraint1, unsatisfiedConstraint2)
                )
            ).success
        )
    }

    @Test
    fun testLogicalConstraintEvaluationXone() {
        assertTrue(
            evaluator.isSatisfied(
                ctx,
                OdrlLogicalConstraint(
                    xone = listOf(unsatisfiedConstraint1, unsatisfiedConstraint2, satisfiedConstraint1)
                )
            ).success
        )
        assertFalse(
            evaluator.isSatisfied(
                ctx,
                OdrlLogicalConstraint(
                    xone = listOf(satisfiedConstraint1, unsatisfiedConstraint1, satisfiedConstraint2)
                )
            ).success
        )
        assertFalse(
            evaluator.isSatisfied(
                ctx,
                OdrlLogicalConstraint(
                    xone = listOf(unsatisfiedConstraint1, unsatisfiedConstraint2)
                )
            ).success
        )
    }

    @Test
    fun testLogicalConstraintEvaluationAnd() {
        assertFalse(
            evaluator.isSatisfied(
                ctx,
                OdrlLogicalConstraint(
                    and = listOf(unsatisfiedConstraint1, unsatisfiedConstraint2, satisfiedConstraint1)
                )
            ).success
        )
        assertTrue(
            evaluator.isSatisfied(
                ctx,
                OdrlLogicalConstraint(
                    and = listOf(satisfiedConstraint1, satisfiedConstraint2)
                )
            ).success
        )
        assertFalse(
            evaluator.isSatisfied(
                ctx,
                OdrlLogicalConstraint(
                    and = listOf(unsatisfiedConstraint1, unsatisfiedConstraint2)
                )
            ).success
        )
    }

    @Test
    fun testLogicalConstraintEvaluationAndSequence() {
        assertFalse(
            evaluator.isSatisfied(
                ctx,
                OdrlLogicalConstraint(
                    andSequence = listOf(unsatisfiedConstraint1, unsatisfiedConstraint2, satisfiedConstraint1)
                )
            ).success
        )
        assertTrue(
            evaluator.isSatisfied(
                ctx,
                OdrlLogicalConstraint(
                    andSequence = listOf(satisfiedConstraint1, satisfiedConstraint2)
                )
            ).success
        )
        assertFalse(
            evaluator.isSatisfied(
                ctx,
                OdrlLogicalConstraint(
                    andSequence = listOf(unsatisfiedConstraint1, unsatisfiedConstraint2)
                )
            ).success
        )
    }
}