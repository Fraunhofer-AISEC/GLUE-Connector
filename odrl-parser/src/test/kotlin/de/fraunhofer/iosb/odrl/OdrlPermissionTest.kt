package de.fraunhofer.iosb.odrl

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import de.fraunhofer.iosb.odrl.Commons.Companion.loadFileOrDie
import org.junit.jupiter.api.assertThrows
import java.io.StringReader
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals

class OdrlPermissionTest {
    private val mapper = Commons.parserInstance.mapper
    private val permission1 = OdrlPermission(
        uid = URI.create("http://example.com/permission"),
        action = OdrlAction("http://www.w3.org/ns/odrl/2/play"),
        target = OdrlAsset("http://example.com/targetAsset"),
        output = OdrlAsset("http://example.com/outputAsset"),
        assignee = OdrlParty("http://example.com/assignee"),
        assigner = OdrlParty("http://example.com/assigner"),
        constraint = listOf(
            OdrlConstraint(
                leftOperand = OdrlLeftOperand("http://example.com/leftOperand"),
                operator = OdrlOperator("http://www.w3.org/ns/odrl/2/gteq"),
                rightOperand = listOf(PrimitiveString("exampleRightOperand"))
            )
        ),
        duty = listOf(
            OdrlDuty(
                uid = URI.create("http://example.com/compensationDuty"),
                action = OdrlAction("http://www.w3.org/ns/odrl/2/compensate"),
                compensatedParty = OdrlParty("http://example.com/compensatedParty")
            ),
            OdrlDuty(
                uid = URI.create("http://example.com/attributionDuty"),
                action = OdrlAction("http://www.w3.org/ns/odrl/2/attribute"),
                attributedParty = OdrlParty("http://example.com/attributedParty")
            )
        )
    )

    @Test
    fun testPermissionDeserialization() {
        val permission = mapper.readValue(loadFileOrDie("/permission-example-1.json"), OdrlPermission::class.java)
        assertEquals(permission1, permission)
    }

    @Test
    fun testPermissionSerializationAndDeserialization() {
        val string = mapper.writeValueAsString(permission1)
        val permission = mapper.readValue(StringReader(string), OdrlPermission::class.java)
        assertEquals(permission1, permission)
    }

    @Test
    fun testPermissionDeserializationWithInvalidUri() {
        assertThrows<InvalidFormatException> {
            mapper.readValue(
                "{\n" +
                        "  \"@context\": \"http://www.w3.org/ns/odrl.jsonld\",\n" +
                        "  \"@type\": \"Permission\",\n" +
                        "  \"@id\": \":::invalid-uri:::\"\n" +
                        "}", OdrlPermission::class.java
            )
        }
    }
}