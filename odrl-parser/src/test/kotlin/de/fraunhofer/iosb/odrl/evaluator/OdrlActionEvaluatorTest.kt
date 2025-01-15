package de.fraunhofer.iosb.odrl.evaluator

import de.fraunhofer.iosb.odrl.*
import kotlinx.coroutines.runBlocking
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OdrlActionEvaluatorTest {
    private val evaluator = OdrlActionEvaluator()
    private val satisfiedConstraint =
        OdrlConstraint(
            leftOperand = OdrlLeftOperand("http://www.w3.org/ns/odrl/2/dateTime"),
            operator = OdrlOperator("http://www.w3.org/ns/odrl/2/gt"),
            rightOperand = listOf(XmlDate(LocalDate.of(2000, 1, 1)))
        )
    private val unsatisfiedConstraint =
        OdrlConstraint(
            leftOperand = OdrlLeftOperand("http://www.w3.org/ns/odrl/2/dateTime"),
            operator = OdrlOperator("http://www.w3.org/ns/odrl/2/lt"),
            rightOperand = listOf(XmlDateTime(LocalDateTime.now().minusHours(1)))
        )

    @Test
    fun testActionMatchingWithNoConstraints() {
        assertTrue(
            evaluator.matches(
                Context("http://example.com/action", "http://example.com/asset"),
                OdrlAction(URI.create("http://example.com/action"))
            ).success
        )
        assertFalse(
            evaluator.matches(
                Context("http://example.com/action", "http://example.com/asset"),
                OdrlAction(URI.create("http://example.com/anotherAction"))
            ).success
        )
    }

    @Test
    fun testActionMatchingWithRefinements() {
        assertTrue(
            evaluator.matches(
                Context("http://example.com/action", "http://example.com/asset"),
                OdrlAction(
                    uid = URI.create("http://example.com/action"),
                    refinement = listOf(satisfiedConstraint)
                )
            ).success
        )
        assertFalse(
            evaluator.matches(
                Context("http://example.com/action", "http://example.com/asset"),
                OdrlAction(
                    uid = URI.create("http://example.com/action"),
                    refinement = listOf(unsatisfiedConstraint)
                )
            ).success
        )
    }

    @Test
    fun testActionExercise() {
        runBlocking {
            assertTrue(
                evaluator.exercise(
                    Context("http://example.com/action", "http://example.com/asset"),
                    OdrlAction(URI.create("http://example.com/action"))
                ) { Result(true) }.success
            )
            assertFalse(
                evaluator.exercise(
                    Context("http://example.com/action", "http://example.com/asset"),
                    OdrlAction(URI.create("http://example.com/action"))
                ) { Result(false) }.success
            )
        }
    }
}