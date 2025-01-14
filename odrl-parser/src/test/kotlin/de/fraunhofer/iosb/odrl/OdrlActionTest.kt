package de.fraunhofer.iosb.odrl

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import de.fraunhofer.iosb.odrl.Commons.Companion.loadFileOrDie
import org.junit.jupiter.api.assertThrows
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals

class OdrlActionTest {
    private val mapper = Commons.parserInstance.mapper
    private val action1 = OdrlAction(
        uid = URI.create("http://example.com/action"),
        refinement = listOf(
            OdrlConstraint(
                leftOperand = OdrlLeftOperand("http://example.com/leftOperand"),
                operator = OdrlOperator("http://www.w3.org/ns/odrl/2/eq"),
                rightOperand = listOf(PrimitiveString("exampleRightOperand"))
            )
        ),
        includedIn = OdrlAction("http://example.com/anotherAction"),
        implies = listOf(
            OdrlAction("http://example.com/impliedAction1"),
            OdrlAction("http://example.com/impliedAction2"),
        )
    )

    @Test
    fun testActionDeserialization() {
        val action = mapper.readValue(loadFileOrDie("/action-example-1.json"), OdrlAction::class.java)
        assertEquals(action1, action)
    }

    @Test
    fun testActionSerializationAndDeserialization() {
        val string = mapper.writeValueAsString(action1)
        val action = mapper.readValue(string, OdrlAction::class.java)
        assertEquals(action1, action)
    }

    @Test
    fun testActionDeserializationWithInvalidUri() {
        assertThrows<InvalidFormatException> {
            mapper.readValue(
                "{\n" +
                        "  \"@context\": \"http://www.w3.org/ns/odrl.jsonld\",\n" +
                        "  \"@type\": \"Action\",\n" +
                        "  \"@id\": \":::invalid-uri:::\"\n" +
                        "}", OdrlAction::class.java
            )
        }
    }
}