package de.fraunhofer.iosb.odrl

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import de.fraunhofer.iosb.odrl.Commons.Companion.loadFileOrDie
import org.junit.jupiter.api.assertThrows
import java.io.StringReader
import java.net.URI
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class OdrlSetTest {
    private val parser = Commons.parserInstance
    private val set1 = OdrlSet(
        uid = URI.create("http://example.com/policy"),
        profile = listOf(OdrlIri("http://example.com/profile")),
        permission = listOf(
            OdrlPermission(
                uid = URI.create("http://example.com/permission"),
                target = OdrlAsset("http://example.com/target"),
                action = OdrlAction("http://www.w3.org/ns/odrl/2/play"),
                constraint = listOf(
                    OdrlConstraint(
                        leftOperand = OdrlLeftOperand("http://www.w3.org/ns/odrl/2/dateTime"),
                        operator = OdrlOperator("http://www.w3.org/ns/odrl/2/lteq"),
                        rightOperand = listOf(XmlDate(LocalDate.of(2045, 12, 31)))
                    )
                )
            )
        )
    )

    @Test
    fun testSetDeserialization() {
        val set = parser.readOdrlPolicy(loadFileOrDie("/set-example-1.json"))
        assertEquals(set1, set)
    }

    @Test
    fun testSetSerializationAndDeserialization() {
        val string = parser.writeOdrlPolicy(set1)
        val set = parser.readOdrlPolicy(StringReader(string))
        assertEquals(set1, set)
    }

    @Test
    fun testSetDeserializationWithInvalidUri() {
        assertThrows<InvalidFormatException> {
            parser.readOdrlPolicy(
                StringReader(
                    "{\n" +
                            "  \"@context\": \"http://www.w3.org/ns/odrl.jsonld\",\n" +
                            "  \"@type\": \"Set\",\n" +
                            "  \"@id\": \":::invalid-uri:::\"\n" +
                            "}"
                )
            )
        }
    }

    @Test
    fun testSetConversionToOffer() {
        val expectedOffer = OdrlOffer(
            uid = URI.create("http://example.com/policy"),
            profile = listOf(OdrlIri("http://example.com/profile")),
            permission = listOf(
                OdrlPermission(
                    uid = URI.create("http://example.com/permission"),
                    target = OdrlAsset("http://example.com/target"),
                    assigner = OdrlParty("http://example.com/assigner1"),
                    action = OdrlAction("http://www.w3.org/ns/odrl/2/play"),
                    constraint = listOf(
                        OdrlConstraint(
                            leftOperand = OdrlLeftOperand("http://www.w3.org/ns/odrl/2/dateTime"),
                            operator = OdrlOperator("http://www.w3.org/ns/odrl/2/lteq"),
                            rightOperand = listOf(XmlDate(LocalDate.of(2045, 12, 31)))
                        )
                    )
                ),
                OdrlPermission(
                    uid = URI.create("http://example.com/permission"),
                    target = OdrlAsset("http://example.com/target"),
                    assigner = OdrlParty("http://example.com/assigner2"),
                    action = OdrlAction("http://www.w3.org/ns/odrl/2/play"),
                    constraint = listOf(
                        OdrlConstraint(
                            leftOperand = OdrlLeftOperand("http://www.w3.org/ns/odrl/2/dateTime"),
                            operator = OdrlOperator("http://www.w3.org/ns/odrl/2/lteq"),
                            rightOperand = listOf(XmlDate(LocalDate.of(2045, 12, 31)))
                        )
                    )
                )
            )
        )
        assertEquals(
            expectedOffer,
            parser.convertToOdrlOffer(
                set1,
                assigners = listOf(OdrlParty("http://example.com/assigner1"), OdrlParty("http://example.com/assigner2"))
            )
        )
    }

    @Test
    fun testSetConversionToAgreement() {
        val expectedAgreement = OdrlAgreement(
            uid = URI.create("http://example.com/policy"),
            profile = listOf(OdrlIri("http://example.com/profile")),
            permission = listOf(
                OdrlPermission(
                    uid = URI.create("http://example.com/permission"),
                    target = OdrlAsset("http://example.com/target"),
                    assigner = OdrlParty("http://example.com/assigner"),
                    assignee = OdrlParty("http://example.com/assignee"),
                    action = OdrlAction("http://www.w3.org/ns/odrl/2/play"),
                    constraint = listOf(
                        OdrlConstraint(
                            leftOperand = OdrlLeftOperand("http://www.w3.org/ns/odrl/2/dateTime"),
                            operator = OdrlOperator("http://www.w3.org/ns/odrl/2/lteq"),
                            rightOperand = listOf(XmlDate(LocalDate.of(2045, 12, 31)))
                        )
                    )
                )
            )
        )
        assertEquals(
            expectedAgreement,
            parser.convertToOdrlAgreement(
                set1,
                OdrlParty("http://example.com/assignee"),
                OdrlParty("http://example.com/assigner")
            )
        )
    }
}