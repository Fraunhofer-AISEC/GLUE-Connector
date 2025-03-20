package de.fraunhofer.iosb.odrl.evaluator

import de.fraunhofer.iosb.odrl.*
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OdrlProhibitionEvaluatorTest {
    private val evaluator = OdrlProhibitionEvaluator()
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
    fun testProhibitionWithoutMatchingAction() {
        runBlocking {
            // Prohibitions without matching actions do not disallow context
            assertFalse(
                evaluator.isDisallowed(
                    ctx,
                    OdrlProhibition(
                        action = OdrlAction("http://example.com/anotherAction"),
                        target = OdrlAsset("http://example.com/asset")
                    )
                ).success
            )
        }
    }

    @Test
    fun testProhibitionWithoutMatchingTarget() {
        runBlocking {
            // Prohibitions without matching target do not disallow context
            assertFalse(
                evaluator.isDisallowed(
                    ctx,
                    OdrlProhibition(
                        action = OdrlAction("http://example.com/action"),
                        target = OdrlAsset("http://example.com/anotherAsset")
                    )
                ).success
            )
        }
    }

    @Test
    fun testProhibitionWithUnsatisfiedConstraints() {
        runBlocking {
            // Prohibitions with unsatisfied constraints do not disallow context
            assertFalse(
                evaluator.isDisallowed(
                    ctx,
                    OdrlProhibition(
                        action = OdrlAction("http://example.com/action"),
                        target = OdrlAsset("http://example.com/asset"),
                        constraint = listOf(satisfiedConstraint, unsatisfiedConstraint)
                    )
                ).success
            )
        }
    }

    @Test
    fun testDisallowedProhibition() {
        runBlocking {
            assertTrue(
                evaluator.isDisallowed(
                    ctx,
                    OdrlProhibition(
                        action = OdrlAction("http://example.com/action"),
                        target = OdrlAsset("http://example.com/asset"),
                        constraint = listOf(satisfiedConstraint)
                    )
                ).success
            )
        }
    }

    @Test
    fun testProhibitionInfringement() {
        runBlocking {
            // Prohibitions with not infringed actions are not infringed
            assertTrue(
                evaluator.isNotInfringed(ctx,
                    OdrlProhibition(
                        action = OdrlAction("http://example.com/action"),
                        target = OdrlAsset("http://example.com/asset"),
                        remedy = listOf(
                            OdrlDuty(
                                action = OdrlAction("http://example.com/remedyAction")
                            )
                        )
                    ),
                    infringementCallback = {
                        // Prohibition action is not infringed
                        it.value.uid.toString() != "http://example.com/action"
                    },
                    actionCallback = {
                        // Prohibition is not infringed, even if remedy action cannot be exercised
                        Result(false)
                    }
                ).success
            )

            // Prohibitions with infringed actions must fulfill remedies in order to be not infringed
            assertFalse(
                evaluator.isNotInfringed(ctx,
                    OdrlProhibition(
                        action = OdrlAction("http://example.com/action"),
                        target = OdrlAsset("http://example.com/asset"),
                        remedy = listOf(
                            OdrlDuty(
                                action = OdrlAction("http://example.com/remedyAction")
                            )
                        )
                    ),
                    infringementCallback = {
                        // Prohibition action is infringed
                        it.value.uid.toString() == "http://example.com/action"
                    },
                    actionCallback = {
                        Result(false) // Remedy action could not be exercised
                    }
                ).success
            )
            assertTrue(
                evaluator.isNotInfringed(ctx,
                    OdrlProhibition(
                        action = OdrlAction("http://example.com/action"),
                        target = OdrlAsset("http://example.com/asset"),
                        remedy = listOf(
                            OdrlDuty(
                                action = OdrlAction("http://example.com/remedyAction")
                            )
                        )
                    ),
                    infringementCallback = {
                        // Prohibition action is infringed
                        it.value.uid.toString() == "http://example.com/action"
                    },
                    actionCallback = {
                        // Remedy action exercised successfully
                        Result(it.value.uid.toString() == "http://example.com/remedyAction")
                    }
                ).success
            )
        }
    }
}