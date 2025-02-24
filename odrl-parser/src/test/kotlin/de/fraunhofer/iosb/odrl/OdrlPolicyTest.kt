package de.fraunhofer.iosb.odrl

import de.fraunhofer.iosb.odrl.Commons.Companion.loadFileOrDie
import org.junit.jupiter.api.assertThrows
import java.io.StringReader
import java.net.URI
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class OdrlPolicyTest {
    private val parser = Commons.parserInstance
    private val policy1 = OdrlPolicy(
        uid = URI.create("http://example.com/policy"),
        profile = listOf(OdrlIri("http://example.com/profile")),
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
    private val compactPolicy1 = OdrlPolicy(
        uid = URI.create("http://example.com/compactPolicy"),
        profile = listOf(OdrlIri("http://example.com/profile")),
        permission = listOf(
            OdrlPermission(
                uid = URI.create("http://example.com/permission1"),
                assignee = OdrlParty("http://example.com/assignee1"),
                assigner = OdrlParty("http://example.com/assigner"),
                target = OdrlAsset("http://example.com/target"),
                action = OdrlAction("http://www.w3.org/ns/odrl/2/play")
            ),
            OdrlPermission(
                uid = URI.create("http://example.com/permission2"),
                assignee = OdrlParty("http://example.com/assignee2"),
                assigner = OdrlParty("http://example.com/assigner"),
                target = OdrlAsset("http://example.com/target"),
                action = OdrlAction("http://www.w3.org/ns/odrl/2/play")
            )
        ),
        prohibition = listOf(OdrlProhibition(
            uid = URI.create("http://example.com/prohibition1"),
            assignee = OdrlParty("http://example.com/assignee3"),
            assigner = OdrlParty("http://example.com/assigner"),
            target = OdrlAsset("http://example.com/target"),
            action = OdrlAction("http://www.w3.org/ns/odrl/2/play")
        ))
    )

    @Test
    fun testPolicyDeserialization() {
        val policy = parser.readOdrlPolicy(loadFileOrDie("/policy-example-1.json"))
        assertEquals(policy1, policy)
    }

    @Test
    fun testPolicySerializationAndDeserialization() {
        val string = parser.writeOdrlPolicy(policy1)
        val policy = parser.readOdrlPolicy(StringReader(string))
        assertEquals(policy1, policy)
    }

    @Test
    fun testPolicyDeserializationWithInvalidUri() {
        assertThrows<IllegalArgumentException> {
            parser.readOdrlPolicy(StringReader(
                "{\n" +
                    "  \"@context\": \"http://www.w3.org/ns/odrl.jsonld\",\n" +
                    "  \"@type\": \"Policy\",\n" +
                    "  \"@id\": \":::invalid-uri:::\"\n" +
                    "}"
            ))
        }
    }

    @Test
    fun testCompactPolicyDeserialization() {
        val policy = parser.readOdrlPolicy(loadFileOrDie("/compact-policy-example-1.json"))
        assertEquals(compactPolicy1, policy)
    }

    @Test
    fun testCompactPolicySerializationAndDeserialization() {
        val string = parser.writeOdrlPolicy(compactPolicy1)
        val policy = parser.readOdrlPolicy(StringReader(string))
        assertEquals(compactPolicy1, policy)
    }
}