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

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import de.fraunhofer.iosb.odrl.Commons.Companion.loadFileOrDie
import org.junit.jupiter.api.assertThrows
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals

class OdrlConstraintTest {
    private val mapper = Commons.parserInstance.mapper
    private val constraints =
        listOf(
            OdrlConstraint(
                uid = URI.create("http://example.com/constraint"),
                leftOperand = OdrlLeftOperand("http://example.com/leftOperand"),
                operator = OdrlOperator("http://www.w3.org/ns/odrl/2/eq"),
                rightOperand =
                    listOf(
                        PrimitiveString("exampleRightOperand1"),
                        PrimitiveString("exampleRightOperand2"),
                    ),
                dataType = XmlAnyType("http://example.com/dataType"),
                unit = OdrlIri("http://example.com/unit"),
                status = PrimitiveString("http://example.com/status"),
            ),
            OdrlConstraint(
                uid = URI.create("http://example.com/constraint"),
                leftOperand = OdrlLeftOperand("http://example.com/leftOperand"),
                operator = OdrlOperator("http://www.w3.org/ns/odrl/2/eq"),
                rightOperandReference =
                    listOf(
                        XmlAnyUri("http://example.com/rightOperand1"),
                        XmlAnyUri("http://example.com/rightOperand2"),
                    ),
                dataType = XmlAnyType("http://example.com/dataType"),
                unit = OdrlIri("http://example.com/unit"),
                status = PrimitiveString("http://example.com/status"),
            ),
        )

    @Test
    fun testConstraintDeserialization() {
        constraints.forEachIndexed { i, c ->
            val constraint =
                mapper.readValue(loadFileOrDie("/constraint-example-${i + 1}.json"), OdrlConstraint::class.java)
            assertEquals(c, constraint)
        }
    }

    @Test
    fun testConstraintSerializationAndDeserialization() {
        constraints.forEach { c ->
            val string = mapper.writeValueAsString(c)
            val constraint = mapper.readValue(string, OdrlConstraint::class.java)
            assertEquals(c, constraint)
        }
    }

    @Test
    fun testConstraintDeserializationWithInvalidUri() {
        assertThrows<InvalidFormatException> {
            mapper.readValue(
                "{\n" +
                    "  \"@context\": \"http://www.w3.org/ns/odrl.jsonld\",\n" +
                    "  \"@type\": \"Constraint\",\n" +
                    "  \"@id\": \":::invalid-uri:::\"\n" +
                    "}",
                OdrlConstraint::class.java,
            )
        }
    }
}
