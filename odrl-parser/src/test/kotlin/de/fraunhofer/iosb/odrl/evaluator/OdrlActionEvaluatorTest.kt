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

class OdrlActionEvaluatorTest {
    private val evaluator = OdrlActionEvaluator()
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
    fun testActionMatchingWithNoConstraints() {
        assertTrue(
            evaluator
                .matches(
                    Context("http://example.com/action", "http://example.com/asset"),
                    OdrlAction(RdfValue("http://example.com/action")),
                ).success,
        )
        assertFalse(
            evaluator
                .matches(
                    Context("http://example.com/action", "http://example.com/asset"),
                    OdrlAction(RdfValue("http://example.com/anotherAction")),
                ).success,
        )
    }

    @Test
    fun testActionMatchingWithRefinements() {
        assertTrue(
            evaluator
                .matches(
                    Context("http://example.com/action", "http://example.com/asset"),
                    OdrlAction(
                        value = RdfValue("http://example.com/action"),
                        refinement = listOf(satisfiedConstraint),
                    ),
                ).success,
        )
        assertFalse(
            evaluator
                .matches(
                    Context("http://example.com/action", "http://example.com/asset"),
                    OdrlAction(
                        value = RdfValue("http://example.com/action"),
                        refinement = listOf(unsatisfiedConstraint),
                    ),
                ).success,
        )
    }

    @Test
    fun testActionExercise() {
        runBlocking {
            assertTrue(
                evaluator
                    .exercise(
                        Context("http://example.com/action", "http://example.com/asset"),
                        OdrlAction(RdfValue("http://example.com/action")),
                    ) { Result(true) }
                    .success,
            )
            assertFalse(
                evaluator
                    .exercise(
                        Context("http://example.com/action", "http://example.com/asset"),
                        OdrlAction(RdfValue("http://example.com/action")),
                    ) { Result(false) }
                    .success,
            )
        }
    }
}
