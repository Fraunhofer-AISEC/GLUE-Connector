package de.fraunhofer.iosb.odrl

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import de.fraunhofer.iosb.odrl.Commons.Companion.loadFileOrDie
import org.junit.jupiter.api.assertThrows
import java.io.StringReader
import java.net.URI
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class OdrlPolicyTest {
    private val parser = Commons.parserInstance
    private val compactParser = Commons.compactParserInstance
    private val policy1 = OdrlPolicy(
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
        prohibition = listOf(
            OdrlProhibition(
                uid = URI.create("http://example.com/prohibition1"),
                assignee = OdrlParty("http://example.com/assignee3"),
                assigner = OdrlParty("http://example.com/assigner"),
                target = OdrlAsset("http://example.com/target"),
                action = OdrlAction("http://www.w3.org/ns/odrl/2/play")
            )
        )
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
        assertThrows<InvalidFormatException> {
            parser.readOdrlPolicy(
                StringReader(
                    "{\n" +
                            "  \"@context\": \"http://www.w3.org/ns/odrl.jsonld\",\n" +
                            "  \"@type\": \"Policy\",\n" +
                            "  \"@id\": \":::invalid-uri:::\"\n" +
                            "}"
                )
            )
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

    private fun normalizeString(s: String): String {
        return s.replace("\n", "").replace(" ", "")
    }

    @Test
    fun testCompactPolicyCompactSerialization() {
        val string = compactParser.writeOdrlPolicy(compactPolicy1)
        val expected = "{\n" +
                "  \"type\": \"Policy\",\n" +
                "  \"uid\": \"http://example.com/compactPolicy\",\n" +
                "  \"permission\": [\n" +
                "    {\n" +
                "      \"uid\": \"http://example.com/permission1\",\n" +
                "      \"assignee\": {\n" +
                "        \"type\": \"Party\",\n" +
                "        \"uid\": \"http://example.com/assignee1\"\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"uid\": \"http://example.com/permission2\",\n" +
                "      \"assignee\": {\n" +
                "        \"type\": \"Party\",\n" +
                "        \"uid\": \"http://example.com/assignee2\"\n" +
                "      }\n" +
                "    }\n" +
                "  ],\n" +
                "  \"prohibition\": {\n" +
                "    \"uid\": \"http://example.com/prohibition1\",\n" +
                "    \"assignee\": {\n" +
                "      \"type\": \"Party\",\n" +
                "      \"uid\": \"http://example.com/assignee3\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"profile\": \"http://example.com/profile\",\n" +
                "  \"odrl:action\": \"http://www.w3.org/ns/odrl/2/play\",\n" +
                "  \"target\": {\n" +
                "    \"type\": \"Asset\",\n" +
                "    \"uid\": \"http://example.com/target\"\n" +
                "  },\n" +
                "  \"assigner\": {\n" +
                "    \"type\": \"Party\",\n" +
                "    \"uid\": \"http://example.com/assigner\"\n" +
                "  },\n" +
                "  \"@context\": \"http://www.w3.org/ns/odrl.jsonld\"\n" +
                "}"
        assertEquals(normalizeString(expected), normalizeString(string))
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
    fun testCompositionPolicy1CompactSerialization() {
        val string = compactParser.writeOdrlPolicy(compositionPolicy1)
        val expected = "{\n" +
                "  \"type\": \"Policy\",\n" +
                "  \"uid\": \"http://example.com/compositionPolicy\",\n" +
                "  \"permission\": {},\n" +
                "  \"profile\": \"http://example.com/profile\",\n" +
                "  \"odrl:action\": [\n" +
                "    \"http://www.w3.org/ns/odrl/2/play\",\n" +
                "    \"http://www.w3.org/ns/odrl/2/stream\"\n" +
                "  ],\n" +
                "  \"target\": [\n" +
                "    {\n" +
                "      \"type\": \"Asset\",\n" +
                "      \"uid\": \"http://example.com/target1\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"type\": \"Asset\",\n" +
                "      \"uid\": \"http://example.com/target2\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"assigner\": {\n" +
                "    \"type\": \"Party\",\n" +
                "    \"uid\": \"http://example.com/assigner\"\n" +
                "  },\n" +
                "  \"@context\": \"http://www.w3.org/ns/odrl.jsonld\"\n" +
                "}"
        assertEquals(normalizeString(expected), normalizeString(string))
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

    @Test
    fun testCompositionPolicy2CompactSerialization() {
        val string = compactParser.writeOdrlPolicy(compositionPolicy2)
        val expected = "{\n" +
                "  \"type\": \"Policy\",\n" +
                "  \"uid\": \"http://example.com/compositionPolicy\",\n" +
                "  \"permission\": {\n" +
                "    \"odrl:action\": \"http://www.w3.org/ns/odrl/2/use\"\n" +
                "  },\n" +
                "  \"prohibition\": {\n" +
                "    \"odrl:action\": [\n" +
                "      \"http://www.w3.org/ns/odrl/2/play\",\n" +
                "      \"http://www.w3.org/ns/odrl/2/stream\"\n" +
                "    ]\n" +
                "  },\n" +
                "  \"profile\": \"http://example.com/profile\",\n" +
                "  \"target\": [\n" +
                "    {\n" +
                "      \"type\": \"Asset\",\n" +
                "      \"uid\": \"http://example.com/target1\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"type\": \"Asset\",\n" +
                "      \"uid\": \"http://example.com/target2\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"assigner\": {\n" +
                "    \"type\": \"Party\",\n" +
                "    \"uid\": \"http://example.com/assigner\"\n" +
                "  },\n" +
                "  \"@context\": \"http://www.w3.org/ns/odrl.jsonld\"\n" +
                "}"
        assertEquals(normalizeString(expected), normalizeString(string))
    }

    @Test
    fun testCompositionPolicy2ConversionToOffer() {
        val expectedOffer = OdrlOffer(
            uid = URI.create("http://example.com/compositedOffer"),
            profile = listOf(OdrlIri("http://example.com/profile")),
            permission = listOf(
                OdrlPermission(
                    assigner = OdrlParty("http://example.com/freshAssigner"),
                    target = OdrlAsset("http://example.com/target1"),
                    action = OdrlAction("http://www.w3.org/ns/odrl/2/use")
                ),
                OdrlPermission(
                    assigner = OdrlParty("http://example.com/freshAssigner"),
                    target = OdrlAsset("http://example.com/target2"),
                    action = OdrlAction("http://www.w3.org/ns/odrl/2/use")
                )
            ),
            prohibition = listOf(
                OdrlProhibition(
                    assigner = OdrlParty("http://example.com/freshAssigner"),
                    target = OdrlAsset("http://example.com/target1"),
                    action = OdrlAction("http://www.w3.org/ns/odrl/2/play")
                ),
                OdrlProhibition(
                    assigner = OdrlParty("http://example.com/freshAssigner"),
                    target = OdrlAsset("http://example.com/target2"),
                    action = OdrlAction("http://www.w3.org/ns/odrl/2/play")
                ),
                OdrlProhibition(
                    assigner = OdrlParty("http://example.com/freshAssigner"),
                    target = OdrlAsset("http://example.com/target1"),
                    action = OdrlAction("http://www.w3.org/ns/odrl/2/stream")
                ),
                OdrlProhibition(
                    assigner = OdrlParty("http://example.com/freshAssigner"),
                    target = OdrlAsset("http://example.com/target2"),
                    action = OdrlAction("http://www.w3.org/ns/odrl/2/stream")
                )
            )
        )
        assertEquals(
            expectedOffer,
            parser.convertToOdrlOffer(
                compositionPolicy2,
                OdrlParty("http://example.com/freshAssigner"),
                URI.create("http://example.com/compositedOffer")
            )
        )
    }

    @Test
    fun testCompositionPolicy2ConversionToAgreement() {
        val expectedAgreement = OdrlAgreement(
            uid = URI.create("http://example.com/compositionPolicy"),
            profile = listOf(OdrlIri("http://example.com/profile")),
            permission = listOf(
                OdrlPermission(
                    assigner = OdrlParty("http://example.com/assigner"),
                    assignee = OdrlParty("http://example.com/assignee"),
                    target = OdrlAsset("http://example.com/target1"),
                    action = OdrlAction("http://www.w3.org/ns/odrl/2/use")
                ),
                OdrlPermission(
                    assigner = OdrlParty("http://example.com/assigner"),
                    assignee = OdrlParty("http://example.com/assignee"),
                    target = OdrlAsset("http://example.com/target2"),
                    action = OdrlAction("http://www.w3.org/ns/odrl/2/use")
                )
            ),
            prohibition = listOf(
                OdrlProhibition(
                    assigner = OdrlParty("http://example.com/assigner"),
                    assignee = OdrlParty("http://example.com/assignee"),
                    target = OdrlAsset("http://example.com/target1"),
                    action = OdrlAction("http://www.w3.org/ns/odrl/2/play")
                ),
                OdrlProhibition(
                    assigner = OdrlParty("http://example.com/assigner"),
                    assignee = OdrlParty("http://example.com/assignee"),
                    target = OdrlAsset("http://example.com/target2"),
                    action = OdrlAction("http://www.w3.org/ns/odrl/2/play")
                ),
                OdrlProhibition(
                    assigner = OdrlParty("http://example.com/assigner"),
                    assignee = OdrlParty("http://example.com/assignee"),
                    target = OdrlAsset("http://example.com/target1"),
                    action = OdrlAction("http://www.w3.org/ns/odrl/2/stream")
                ),
                OdrlProhibition(
                    assigner = OdrlParty("http://example.com/assigner"),
                    assignee = OdrlParty("http://example.com/assignee"),
                    target = OdrlAsset("http://example.com/target2"),
                    action = OdrlAction("http://www.w3.org/ns/odrl/2/stream")
                )
            )
        )
        assertEquals(
            expectedAgreement,
            parser.convertToOdrlAgreement(
                compositionPolicy2,
                listOf(OdrlParty("http://example.com/assignee"))
            )
        )
    }
}