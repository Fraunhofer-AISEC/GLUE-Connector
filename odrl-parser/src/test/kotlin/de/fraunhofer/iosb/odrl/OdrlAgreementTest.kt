package de.fraunhofer.iosb.odrl

import com.fasterxml.jackson.databind.JsonMappingException
import de.fraunhofer.iosb.odrl.Commons.Companion.loadFileOrDie
import org.junit.jupiter.api.assertThrows
import java.io.StringReader
import java.net.URI
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class OdrlAgreementTest {
    private val parser = Commons.parserInstance
    private val agreement1 = OdrlAgreement(
        uid = URI.create("http://example.com/agreement"),
        profile = listOf(OdrlIri(URI.create("http://example.com/profile"))),
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

    @Test
    fun testAgreementDeserialization() {
        val agreement = parser.readOdrlPolicy(loadFileOrDie("/agreement-example-1.json"))
        assertEquals(agreement1, agreement)
    }

    @Test
    fun testAgreementSerializationAndDeserialization() {
        val string = parser.writeOdrlPolicy(agreement1)
        val agreement = parser.readOdrlPolicy(StringReader(string))
        assertEquals(agreement1, agreement)
    }

    @Test
    fun testAgreementDeserializationWithInvalidUri() {
        assertThrows<IllegalArgumentException> {
            parser.readOdrlPolicy(StringReader(
                "{\n" +
                    "  \"@context\": \"http://www.w3.org/ns/odrl.jsonld\",\n" +
                    "  \"@type\": \"Agreement\",\n" +
                    "  \"@id\": \":::invalid-uri:::\"\n" +
                    "}"
            ))
        }
    }

    @Test
    fun testAgreementDeserializationWithoutAssigner() {
        assertThrows<JsonMappingException> {
            parser.readOdrlPolicy(StringReader(
                "{\n" +
                    "  \"@context\": \"http://www.w3.org/ns/odrl.jsonld\",\n" +
                    "  \"@type\": \"Agreement\",\n" +
                    "  \"permission\": {\n" +
                    "    \"target\": \"http://example.com/target\",\n" +
                    "    \"action\": \"play\",\n" +
                    "    \"assignee\": \"http://example.com/assignee\"\n" +
                    "  }\n" +
                    "}"
            ))
        }
    }

    @Test
    fun testAgreementDeserializationWithoutAssignee() {
        assertThrows<JsonMappingException> {
            parser.readOdrlPolicy(StringReader(
                "{\n" +
                    "  \"@context\": \"http://www.w3.org/ns/odrl.jsonld\",\n" +
                    "  \"@type\": \"Agreement\",\n" +
                    "  \"prohibition\": {\n" +
                    "    \"target\": \"http://example.com/target\",\n" +
                    "    \"action\": \"play\",\n" +
                    "    \"assigner\": \"http://example.com/assigner\"\n" +
                    "  }\n" +
                    "}"
            ))
        }
    }
}