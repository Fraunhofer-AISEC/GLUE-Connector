package de.fraunhofer.iosb.odrl.evaluator

import de.fraunhofer.iosb.odrl.*
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OdrlPermissionEvaluatorTest {
    private val evaluator = OdrlPermissionEvaluator()
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
    fun testPermissionWithoutMatchingAction() {
        runBlocking {
            // Permissions without matching actions do not allow context
            assertFalse(
                evaluator.isAllowed(
                    ctx,
                    OdrlPermission(
                        action = OdrlAction("http://example.com/anotherAction"),
                        target = OdrlAsset("http://example.com/asset")
                    )
                ).success
            )
        }
    }

    @Test
    fun testPermissionWithoutMatchingTarget() {
        runBlocking {
            // Permissions without matching target do not allow context
            assertFalse(
                evaluator.isAllowed(
                    ctx,
                    OdrlPermission(
                        action = OdrlAction("http://example.com/action"),
                        target = OdrlAsset("http://example.com/anotherAsset")
                    )
                ).success
            )
        }
    }

    @Test
    fun testPermissionWithUnsatisfiedConstraints() {
        runBlocking {
            // Permissions with unsatisfied constraints do not allow context
            assertFalse(
                evaluator.isAllowed(
                    ctx,
                    OdrlPermission(
                        action = OdrlAction("http://example.com/action"),
                        target = OdrlAsset("http://example.com/asset"),
                        constraint = listOf(satisfiedConstraint, unsatisfiedConstraint)
                    )
                ).success
            )
        }
    }

    @Test
    fun testPermissionWithUnfulfilledDuties() {
        runBlocking {
            // Permissions with unfulfilled duties do not allow context
            assertFalse(
                evaluator.isAllowed(
                    ctx,
                    OdrlPermission(
                        action = OdrlAction("http://example.com/action"),
                        target = OdrlAsset("http://example.com/asset"),
                        constraint = listOf(satisfiedConstraint),
                        duty = listOf(
                            OdrlDuty(
                                action = OdrlAction("http://example.com/dutyAction")
                            )
                        )
                    )
                ) {
                    Result(false) // Duty action could not be exercised
                }.success
            )
        }
    }

    @Test
    fun testAllowedPermission() {
        runBlocking {
            assertTrue(
                evaluator.isAllowed(
                    ctx,
                    OdrlPermission(
                        action = OdrlAction("http://example.com/action"),
                        target = OdrlAsset("http://example.com/asset"),
                        constraint = listOf(satisfiedConstraint),
                        duty = listOf(
                            OdrlDuty(
                                action = OdrlAction("http://example.com/dutyAction")
                            )
                        )
                    )
                ) {
                    // Duty action exercised successfully
                    Result(it.value.uid.toString() == "http://example.com/dutyAction")
                }.success
            )
        }
    }
}