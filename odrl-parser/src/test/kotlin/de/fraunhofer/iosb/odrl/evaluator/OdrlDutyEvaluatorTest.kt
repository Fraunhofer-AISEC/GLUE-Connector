package de.fraunhofer.iosb.odrl.evaluator

import de.fraunhofer.iosb.odrl.*
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OdrlDutyEvaluatorTest {
    private val evaluator = OdrlDutyEvaluator()
    private val ctx = Context("http://example.com/action", "http://example.com/asset")
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
    fun testDutyWithoutMatchingTarget() {
        runBlocking {
            // Duties without matching targets are considered fulfilled
            assertTrue(
                evaluator.isFulfilled(ctx,
                    OdrlDuty(target = OdrlAsset("http://example.com/anotherAsset"))
                ).success
            )
        }
    }

    @Test
    fun testDutyWithUnsatisfiedConstraints() {
        runBlocking {
            // Duties without satisfied constraints are considered fulfilled
            assertTrue(
                evaluator.isFulfilled(ctx,
                    OdrlDuty(
                        target = OdrlAsset("http://example.com/asset"),
                        constraint = listOf(satisfiedConstraint, unsatisfiedConstraint)
                    )
                ).success
            )
        }
    }

    @Test
    fun testDutyWithExercisedAction() {
        runBlocking {
            // Duty is fulfilled if action is exercised
            assertTrue(
                evaluator.isFulfilled(ctx,
                    OdrlDuty(
                        target = OdrlAsset("http://example.com/asset"),
                        constraint = listOf(satisfiedConstraint),
                        action = OdrlAction("http://example.com/actionToBeExercised")
                    )
                ) {
                    // Duty action exercised successfully
                    Result(it.value.uid.toString() == "http://example.com/actionToBeExercised")
                }.success
            )

            // If action could not be exercised, consequences have to be fulfilled
            val result =
                evaluator.isFulfilled(
                    Context("http://example.com/action", "http://example.com/asset"),
                    OdrlDuty(
                        target = OdrlAsset("http://example.com/asset"),
                        constraint = listOf(satisfiedConstraint),
                        action = OdrlAction("http://example.com/actionToBeExercised"),
                        consequence = listOf(
                            OdrlDuty(
                                target = OdrlAsset("http://example.com/asset"),
                                action = OdrlAction("http://example.com/consequenceAction"),
                            )
                        )
                    )
                ) {
                    Result(false) // Both duty action and consequence action could not be exercised
                }
            assertFalse(result.success)
            assertTrue(result.failureReasoning.size >= 1)
            assertTrue(result.failureReasoning[0].contains("Not all consequences fulfilled"))
        }
    }
}