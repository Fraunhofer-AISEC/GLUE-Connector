package de.fraunhofer.iosb.odrl

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import de.fraunhofer.iosb.odrl.Commons.Companion.loadFileOrDie
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

class OdrlPartyCollectionTest {
    private val mapper = Commons.parserInstance.mapper
    private val partyCollection1 = OdrlPartyCollection(
        source = OdrlIri("http://example.com/partyCollection"),
        refinement = listOf(
            OdrlConstraint(
                leftOperand = OdrlLeftOperand("http://example.com/leftOperand1"),
                operator = OdrlOperator("http://www.w3.org/ns/odrl/2/eq"),
                rightOperand = listOf(PrimitiveString("exampleRightOperand1"))
            ),
            OdrlConstraint(
                leftOperand = OdrlLeftOperand("http://example.com/leftOperand2"),
                operator = OdrlOperator("http://www.w3.org/ns/odrl/2/eq"),
                rightOperand = listOf(PrimitiveString("exampleRightOperand2"))
            )
        )
    )

    @Test
    fun testPartyCollectionDeserialization() {
        val partyCollection =
            mapper.readValue(loadFileOrDie("/party-collection-example-1.json"), OdrlPartyCollection::class.java)
        assertEquals(partyCollection1, partyCollection)
    }

    @Test
    fun testPartyCollectionSerializationAndDeserialization() {
        val string = mapper.writeValueAsString(partyCollection1)
        val partyCollection = mapper.readValue(string, OdrlPartyCollection::class.java)
        assertEquals(partyCollection1, partyCollection)
    }

    @Test
    fun testPartyCollectionDeserializationWithInvalidSource() {
        assertThrows<InvalidFormatException> {
            mapper.readValue(
                "{\n" +
                        "  \"@context\": \"http://www.w3.org/ns/odrl.jsonld\",\n" +
                        "  \"@type\": \"PartyCollection\",\n" +
                        "  \"source\": \":::invalid-uri:::\"\n" +
                        "}", OdrlPartyCollection::class.java
            )
        }
    }
}