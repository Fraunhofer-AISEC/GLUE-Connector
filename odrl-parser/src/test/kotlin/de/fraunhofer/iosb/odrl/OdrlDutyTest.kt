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
import java.io.StringReader
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals

class OdrlDutyTest {
    private val mapper = Commons.parserInstance.mapper
    private val duty1 =
        OdrlDuty(
            uid = URI.create("http://example.com/duty"),
            action = OdrlAction("http://www.w3.org/ns/odrl/2/use"),
            target = OdrlAsset("http://example.com/targetAsset"),
            output = OdrlAsset("http://example.com/outputAsset"),
            assignee = OdrlParty("http://example.com/assignee"),
            assigner = OdrlParty("http://example.com/assigner"),
            constraint =
                listOf(
                    OdrlConstraint(
                        leftOperand = OdrlLeftOperand("http://example.com/leftOperand1"),
                        operator = OdrlOperator("http://www.w3.org/ns/odrl/2/neq"),
                        rightOperand = listOf(PrimitiveString("exampleRightOperand1")),
                    ),
                    OdrlConstraint(
                        leftOperand = OdrlLeftOperand("http://example.com/leftOperand2"),
                        operator = OdrlOperator("http://www.w3.org/ns/odrl/2/neq"),
                        rightOperand = listOf(PrimitiveString("exampleRightOperand2")),
                    ),
                ),
            consequence =
                listOf(
                    OdrlDuty(
                        uid = URI.create("http://example.com/consequenceDuty"),
                        action = OdrlAction("http://www.w3.org/ns/odrl/2/compensate"),
                        compensatedParty = OdrlParty("http://example.com/compensatedParty"),
                    ),
                ),
        )

    @Test
    fun testDutyDeserialization() {
        val duty = mapper.readValue(loadFileOrDie("/duty-example-1.json"), OdrlDuty::class.java)
        assertEquals(duty1, duty)
    }

    @Test
    fun testDutySerializationAndDeserialization() {
        val string = mapper.writeValueAsString(duty1)
        val duty = mapper.readValue(StringReader(string), OdrlDuty::class.java)
        assertEquals(duty1, duty)
    }

    @Test
    fun testDutyDeserializationWithInvalidUri() {
        assertThrows<InvalidFormatException> {
            mapper.readValue(
                "{\n" +
                    "  \"@context\": \"http://www.w3.org/ns/odrl.jsonld\",\n" +
                    "  \"@type\": \"Duty\",\n" +
                    "  \"@id\": \":::invalid-uri:::\"\n" +
                    "}",
                OdrlDuty::class.java,
            )
        }
    }
}
