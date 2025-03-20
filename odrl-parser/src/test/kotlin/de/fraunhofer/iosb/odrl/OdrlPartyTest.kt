package de.fraunhofer.iosb.odrl

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import de.fraunhofer.iosb.odrl.Commons.Companion.loadFileOrDie
import org.junit.jupiter.api.assertThrows
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals

class OdrlPartyTest {
    private val mapper = Commons.parserInstance.mapper
    private val party1 = OdrlParty(
        uid = URI.create("http://example.com/party"),
        partOf = listOf(
            OdrlPartyCollection(
                source = OdrlIri("http://example.com/partyCollection"),
                refinement = listOf(
                    OdrlConstraint(
                        leftOperand = OdrlLeftOperand("http://example.com/leftOperand"),
                        operator = OdrlOperator("http://www.w3.org/ns/odrl/2/eq"),
                        rightOperand = listOf(PrimitiveString("exampleRightOperand"))
                    )
                )
            )
        )
    )

    @Test
    fun testPartyDeserialization() {
        val party = mapper.readValue(loadFileOrDie("/party-example-1.json"), OdrlParty::class.java)
        assertEquals(party1, party)
    }

    @Test
    fun testPartySerializationAndDeserialization() {
        val string = mapper.writeValueAsString(party1)
        val party = mapper.readValue(string, OdrlParty::class.java)
        assertEquals(party1, party)
    }

    @Test
    fun testPartyDeserializationWithInvalidUri() {
        assertThrows<InvalidFormatException> {
            mapper.readValue(
                "{\n" +
                        "  \"@context\": \"http://www.w3.org/ns/odrl.jsonld\",\n" +
                        "  \"@type\": \"Party\",\n" +
                        "  \"@id\": \":::invalid-uri:::\"\n" +
                        "}", OdrlParty::class.java
            )
        }
    }
}