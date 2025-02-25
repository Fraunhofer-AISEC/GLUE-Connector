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
    private val compositionPolicy1 = OdrlPolicy(
        uid = URI.create("http://example.com/compositionPolicy"),
        profile = listOf(OdrlIri("http://example.com/profile")),
        permission = listOf(
            OdrlPermission(
                assigner = OdrlParty("http://example.com/assigner"),
                target = OdrlAsset("http://example.com/target1"),
                action = OdrlAction("http://www.w3.org/ns/odrl/2/play")
            ),
            OdrlPermission(
                assigner = OdrlParty("http://example.com/assigner"),
                target = OdrlAsset("http://example.com/target2"),
                action = OdrlAction("http://www.w3.org/ns/odrl/2/play")
            ),
            OdrlPermission(
                assigner = OdrlParty("http://example.com/assigner"),
                target = OdrlAsset("http://example.com/target1"),
                action = OdrlAction("http://www.w3.org/ns/odrl/2/stream")
            ),
            OdrlPermission(
                assigner = OdrlParty("http://example.com/assigner"),
                target = OdrlAsset("http://example.com/target2"),
                action = OdrlAction("http://www.w3.org/ns/odrl/2/stream")
            )
        )
    )
    private val compositionPolicy2 = OdrlPolicy(
        uid = URI.create("http://example.com/compositionPolicy"),
        profile = listOf(OdrlIri("http://example.com/profile")),
        permission = listOf(
            OdrlPermission(
                assigner = OdrlParty("http://example.com/assigner"),
                target = OdrlAsset("http://example.com/target1"),
                action = OdrlAction("http://www.w3.org/ns/odrl/2/use")
            ),
            OdrlPermission(
                assigner = OdrlParty("http://example.com/assigner"),
                target = OdrlAsset("http://example.com/target2"),
                action = OdrlAction("http://www.w3.org/ns/odrl/2/use")
            )
        ),
        prohibition = listOf(
            OdrlProhibition(
                assigner = OdrlParty("http://example.com/assigner"),
                target = OdrlAsset("http://example.com/target1"),
                action = OdrlAction("http://www.w3.org/ns/odrl/2/play")
            ),
            OdrlProhibition(
                assigner = OdrlParty("http://example.com/assigner"),
                target = OdrlAsset("http://example.com/target2"),
                action = OdrlAction("http://www.w3.org/ns/odrl/2/play")
            ),
            OdrlProhibition(
                assigner = OdrlParty("http://example.com/assigner"),
                target = OdrlAsset("http://example.com/target1"),
                action = OdrlAction("http://www.w3.org/ns/odrl/2/stream")
            ),
            OdrlProhibition(
                assigner = OdrlParty("http://example.com/assigner"),
                target = OdrlAsset("http://example.com/target2"),
                action = OdrlAction("http://www.w3.org/ns/odrl/2/stream")
            )
        )
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

    @Test
    fun testCompositionPolicy1Deserialization() {
        val policy = parser.readOdrlPolicy(loadFileOrDie("/composition-policy-example-1.json"))
        assertEquals(compositionPolicy1, policy)
    }

    @Test
    fun testCompositionPolicy1SerializationAndDeserialization() {
        val string = parser.writeOdrlPolicy(compositionPolicy1)
        val policy = parser.readOdrlPolicy(StringReader(string))
        assertEquals(compositionPolicy1, policy)
    }

    @Test
    fun testCompositionPolicy2Deserialization() {
        val policy = parser.readOdrlPolicy(loadFileOrDie("/composition-policy-example-2.json"))
        assertEquals(compositionPolicy2, policy)
    }

    @Test
    fun testCompositionPolicy2SerializationAndDeserialization() {
        val string = parser.writeOdrlPolicy(compositionPolicy2)
        val policy = parser.readOdrlPolicy(StringReader(string))
        assertEquals(compositionPolicy2, policy)
    }
}