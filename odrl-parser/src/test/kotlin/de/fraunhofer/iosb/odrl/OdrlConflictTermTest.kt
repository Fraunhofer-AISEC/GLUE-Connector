package de.fraunhofer.iosb.odrl

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import org.junit.jupiter.api.assertThrows
import java.io.StringReader
import kotlin.test.Test
import kotlin.test.assertEquals

class OdrlConflictTermTest {
    private val mapper = Commons.parserInstance.mapper

    private fun createConflictTermString(uid: String): String {
        return "{\n" +
                "  \"@context\": \"http://www.w3.org/ns/odrl.jsonld\",\n" +
                "  \"@type\": \"ConflictTerm\",\n" +
                "  \"@id\": \"$uid\"\n" +
                "}"
    }

    @Test
    fun testConflictTermDeserialization() {
        for (s in listOf("perm", "prohibit", "invalid")) {
            val conflictTerm = mapper.readValue(
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
                        "}", OdrlConflictTerm::class.java
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
                        "}", OdrlConflictTerm::class.java
            )
        }
    }
}