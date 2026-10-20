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
package de.fraunhofer.iosb.odrl

import com.fasterxml.jackson.databind.exc.ValueInstantiationException
import de.fraunhofer.iosb.odrl.Commons.Companion.loadFileOrDie
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

class OdrlActionTest {
    private val mapper = Commons.parserInstance.mapper
    private val action1 =
        OdrlAction(
            value = RdfValue("http://example.com/action"),
            includedIn = OdrlAction("http://example.com/anotherAction"),
            implies =
                listOf(
                    OdrlAction("http://example.com/impliedAction1"),
                    OdrlAction("http://example.com/impliedAction2"),
                ),
        )
    private val action2 =
        OdrlAction(
            value = RdfValue("http://www.w3.org/ns/odrl/2/use"),
            refinement =
                listOf(
                    OdrlConstraint(
                        leftOperand = OdrlLeftOperand("http://example.com/leftOperand"),
                        operator = OdrlOperator("http://www.w3.org/ns/odrl/2/eq"),
                        rightOperand = listOf(PrimitiveString("exampleRightOperand")),
                    ),
                ),
            includedIn = OdrlAction("http://example.com/anotherAction"),
            implies =
                listOf(
                    OdrlAction("http://example.com/impliedAction1"),
                    OdrlAction("http://example.com/impliedAction2"),
                ),
        )

    @Test
    fun testAction1Deserialization() {
        val action = mapper.readValue(loadFileOrDie("/action-example-1.json"), OdrlAction::class.java)
        assertEquals(action1, action)
    }

    @Test
    fun testAction1SerializationAndDeserialization() {
        val string = mapper.writeValueAsString(action1)
        val action = mapper.readValue(string, OdrlAction::class.java)
        assertEquals(action1, action)
    }

    @Test
    fun testAction2Deserialization() {
        val action = mapper.readValue(loadFileOrDie("/action-example-2.json"), OdrlAction::class.java)
        assertEquals(action2, action)
    }

    @Test
    fun testAction2SerializationAndDeserialization() {
        val string = mapper.writeValueAsString(action2)
        val action = mapper.readValue(string, OdrlAction::class.java)
        assertEquals(action2, action)
    }

    @Test
    fun testActionDeserializationWithInvalidUri() {
        assertThrows<ValueInstantiationException> {
            mapper.readValue(
                "{\n" +
                    "  \"@context\": \"http://www.w3.org/ns/odrl.jsonld\",\n" +
                    "  \"@type\": \"Action\",\n" +
                    "  \"@id\": \":::invalid-uri:::\"\n" +
                    "}",
                OdrlAction::class.java,
            )
        }
    }
}
