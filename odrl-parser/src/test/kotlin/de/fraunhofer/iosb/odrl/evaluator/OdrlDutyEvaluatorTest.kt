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
            rightOperand = listOf(XmlDate(LocalDate.of(2000, 1, 1))),
        )
    private val unsatisfiedConstraint =
        OdrlConstraint(
            leftOperand = OdrlLeftOperand("http://www.w3.org/ns/odrl/2/dateTime"),
            operator = OdrlOperator("http://www.w3.org/ns/odrl/2/lt"),
            rightOperand = listOf(XmlDateTime(LocalDateTime.now().minusHours(1))),
        )

    @Test
    fun testDutyWithoutMatchingTarget() {
        runBlocking {
            // Duties without matching targets are considered fulfilled
            assertTrue(
                evaluator
                    .isFulfilled(
                        ctx,
                        OdrlDuty(
                            action = OdrlAction(""),
                            target = OdrlAsset("http://example.com/anotherAsset"),
                        ),
                    ).success,
            )
        }
    }

    @Test
    fun testDutyWithUnsatisfiedConstraints() {
        runBlocking {
            // Duties without satisfied constraints are considered fulfilled
            assertTrue(
                evaluator
                    .isFulfilled(
                        ctx,
                        OdrlDuty(
                            action = OdrlAction(""),
                            target = OdrlAsset("http://example.com/asset"),
                            constraint = listOf(satisfiedConstraint, unsatisfiedConstraint),
                        ),
                    ).success,
            )
        }
    }

    @Test
    fun testDutyWithExercisedAction() {
        runBlocking {
            // Duty is fulfilled if action is exercised
            assertTrue(
                evaluator
                    .isFulfilled(
                        ctx,
                        OdrlDuty(
                            target = OdrlAsset("http://example.com/asset"),
                            constraint = listOf(satisfiedConstraint),
                            action = OdrlAction("http://example.com/actionToBeExercised"),
                        ),
                    ) {
                        // Duty action exercised successfully
                        Result(it.value.uid.toString() == "http://example.com/actionToBeExercised")
                    }.success,
            )

            // If action could not be exercised, consequences have to be fulfilled
            val result =
                evaluator.isFulfilled(
                    Context("http://example.com/action", "http://example.com/asset"),
                    OdrlDuty(
                        target = OdrlAsset("http://example.com/asset"),
                        constraint = listOf(satisfiedConstraint),
                        action = OdrlAction("http://example.com/actionToBeExercised"),
                        consequence =
                            listOf(
                                OdrlDuty(
                                    target = OdrlAsset("http://example.com/asset"),
                                    action = OdrlAction("http://example.com/consequenceAction"),
                                ),
                            ),
                    ),
                ) {
                    Result(false) // Both duty action and consequence action could not be exercised
                }
            assertFalse(result.success)
            assertTrue(result.failureReasoning.size >= 1)
            assertTrue(result.failureReasoning[0].contains("Not all consequences fulfilled"))
        }
    }
}
