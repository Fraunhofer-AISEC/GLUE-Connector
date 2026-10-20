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

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import org.junit.jupiter.api.assertThrows
import java.io.StringReader
import kotlin.test.Test
import kotlin.test.assertEquals

class OdrlConflictTermTest {
    private val mapper = Commons.parserInstance.mapper

    private fun createConflictTermString(uid: String): String =
        "{\n" +
            "  \"@context\": \"http://www.w3.org/ns/odrl.jsonld\",\n" +
            "  \"@type\": \"ConflictTerm\",\n" +
            "  \"@id\": \"$uid\"\n" +
            "}"

    @Test
    fun testConflictTermDeserialization() {
        for (s in listOf("perm", "prohibit", "invalid")) {
            val conflictTerm =
                mapper.readValue(
                    StringReader(createConflictTermString("http://www.w3.org/ns/odrl/2/$s")),
                    OdrlConflictTerm::class.java,
                )
            assertEquals(OdrlConflictTerm("http://www.w3.org/ns/odrl/2/$s"), conflictTerm)
        }
    }

    @Test
    fun testActionSerializationAndDeserialization() {
        for (s in listOf("perm", "prohibit", "invalid")) {
            val string = mapper.writeValueAsString(OdrlConflictTerm("http://www.w3.org/ns/odrl/2/$s"))
            val conflictTerm = mapper.readValue(string, OdrlConflictTerm::class.java)
            assertEquals(OdrlConflictTerm("http://www.w3.org/ns/odrl/2/$s"), conflictTerm)
        }
    }

    @Test
    fun testInvalidConflictTermDeserialization() {
        assertThrows<JsonMappingException> {
            mapper.readValue(
                "{\n" +
                    "  \"@context\": \"http://www.w3.org/ns/odrl.jsonld\",\n" +
                    "  \"@type\": \"ConflictTerm\",\n" +
                    "  \"@id\": \"http://www.w3.org/ns/odrl/2/invalid-conflict-term\"\n" +
                    "}",
                OdrlConflictTerm::class.java,
            )
        }
    }

    @Test
    fun testConflictTermDeserializationWithInvalidUri() {
        assertThrows<InvalidFormatException> {
            mapper.readValue(
                "{\n" +
                    "  \"@context\": \"http://www.w3.org/ns/odrl.jsonld\",\n" +
                    "  \"@type\": \"ConflictTerm\",\n" +
                    "  \"@id\": \":::invalid-uri:::\"\n" +
                    "}",
                OdrlConflictTerm::class.java,
            )
        }
    }
}
