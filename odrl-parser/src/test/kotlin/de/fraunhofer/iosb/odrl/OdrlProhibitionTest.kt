package de.fraunhofer.iosb.odrl

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import de.fraunhofer.iosb.odrl.Commons.Companion.loadFileOrDie
import org.junit.jupiter.api.assertThrows
import java.io.StringReader
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals

class OdrlProhibitionTest {
    private val mapper = Commons.parserInstance.mapper
    private val prohibition1 = OdrlProhibition(
        uid = URI.create("http://example.com/prohibition"),
        action = OdrlAction("http://www.w3.org/ns/odrl/2/play"),
        target = OdrlAsset("http://example.com/targetAsset"),
        output = OdrlAsset("http://example.com/outputAsset"),
        assignee = OdrlParty("http://example.com/assignee"),
        assigner = OdrlParty("http://example.com/assigner"),
        constraint = listOf(
            OdrlConstraint(
                leftOperand = OdrlLeftOperand("http://example.com/leftOperand"),
                operator = OdrlOperator("http://www.w3.org/ns/odrl/2/eq"),
                rightOperand = listOf(PrimitiveString("exampleRightOperand"))
            )
        ),
        remedy = listOf(
            OdrlDuty(
                uid = URI.create("http://example.com/compensationDuty"),
                action = OdrlAction("http://www.w3.org/ns/odrl/2/compensate"),
                compensatedParty = OdrlParty("http://example.com/compensatedParty")
            )
        )
    )

    @Test
    fun testProhibitionDeserialization() {
        val prohibition = mapper.readValue(loadFileOrDie("/prohibition-example-1.json"), OdrlProhibition::class.java)
        assertEquals(prohibition1, prohibition)
    }

    @Test
    fun testProhibitionSerializationAndDeserialization() {
        val string = mapper.writeValueAsString(prohibition1)
        val prohibition = mapper.readValue(StringReader(string), OdrlProhibition::class.java)
        assertEquals(prohibition1, prohibition)
    }

    @Test
    fun testProhibitionDeserializationWithInvalidUri() {
        assertThrows<InvalidFormatException> {
            mapper.readValue(
                "{\n" +
                        "  \"@context\": \"http://www.w3.org/ns/odrl.jsonld\",\n" +
                        "  \"@type\": \"Prohibition\",\n" +
                        "  \"@id\": \":::invalid-uri:::\"\n" +
                        "}", OdrlProhibition::class.java
            )
        }
    }
}