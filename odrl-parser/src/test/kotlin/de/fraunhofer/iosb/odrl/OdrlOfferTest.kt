package de.fraunhofer.iosb.odrl

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import de.fraunhofer.iosb.odrl.Commons.Companion.loadFileOrDie
import org.junit.jupiter.api.assertThrows
import java.io.StringReader
import java.net.URI
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class OdrlOfferTest {
    private val parser = Commons.parserInstance
    private val offer1 = OdrlOffer(
        uid = URI.create("http://example.com/policy"),
        profile = listOf(OdrlIri("http://example.com/profile")),
        assigner = OdrlParty("http://example.com/assigner"),
        permission = listOf(OdrlPermission(
            uid = URI.create("http://example.com/permission"),
            target = OdrlAsset("http://example.com/target"),
            action = OdrlAction("http://www.w3.org/ns/odrl/2/play"),
            constraint = listOf(OdrlConstraint(
                leftOperand = OdrlLeftOperand("http://www.w3.org/ns/odrl/2/dateTime"),
                operator = OdrlOperator("http://www.w3.org/ns/odrl/2/lteq"),
                rightOperand = listOf(XmlDate(LocalDate.of(2045, 12, 31)))
            ))
        ))
    )

    @Test
    fun testOfferDeserialization() {
        val offer = parser.readOdrlPolicy(loadFileOrDie("/offer-example-1.json"))
        assertEquals(offer1, offer)
    }

    @Test
    fun testOfferSerializationAndDeserialization() {
        val string = parser.writeOdrlPolicy(offer1)
        val offer = parser.readOdrlPolicy(StringReader(string))
        assertEquals(offer1, offer)
    }

    @Test
    fun testOfferDeserializationWithInvalidUri() {
        assertThrows<InvalidFormatException> {
            parser.readOdrlPolicy(StringReader(
                "{\n" +
                    "  \"@context\": \"http://www.w3.org/ns/odrl.jsonld\",\n" +
                    "  \"@type\": \"Offer\",\n" +
                    "  \"@id\": \":::invalid-uri:::\"\n" +
                    "}"
            ))
        }
    }
}