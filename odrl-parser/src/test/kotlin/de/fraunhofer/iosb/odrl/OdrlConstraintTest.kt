package de.fraunhofer.iosb.odrl

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import de.fraunhofer.iosb.odrl.Commons.Companion.loadFileOrDie
import org.junit.jupiter.api.assertThrows
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals

class OdrlConstraintTest {
    private val mapper = Commons.parserInstance.mapper
    private val constraints = listOf(
        OdrlConstraint(
            uid = URI.create("http://example.com/constraint"),
            leftOperand = OdrlLeftOperand("http://example.com/leftOperand"),
            operator = OdrlOperator("http://www.w3.org/ns/odrl/2/eq"),
            rightOperand = listOf(
                PrimitiveString("exampleRightOperand1"),
                PrimitiveString("exampleRightOperand2"),
            ),
            dataType = XmlAnyType("http://example.com/dataType"),
            unit = OdrlIri("http://example.com/unit"),
            status = PrimitiveString("http://example.com/status"),
        ),
        OdrlConstraint(
            uid = URI.create("http://example.com/constraint"),
            leftOperand = OdrlLeftOperand("http://example.com/leftOperand"),
            operator = OdrlOperator("http://www.w3.org/ns/odrl/2/eq"),
            rightOperandReference = listOf(
                XmlAnyUri("http://example.com/rightOperand1"),
                XmlAnyUri("http://example.com/rightOperand2"),
            ),
            dataType = XmlAnyType("http://example.com/dataType"),
            unit = OdrlIri("http://example.com/unit"),
            status = PrimitiveString("http://example.com/status"),
        )
    )

    @Test
    fun testConstraintDeserialization() {
        for (i in 1..2) {
            val constraint = mapper.readValue(loadFileOrDie("/constraint-example-$i.json"), OdrlConstraint::class.java)
            assertEquals(constraints[i-1], constraint)
        }
    }

    @Test
    fun testConstraintSerializationAndDeserialization() {
        for (c in constraints) {
            val string = mapper.writeValueAsString(c)
            val constraint = mapper.readValue(string, OdrlConstraint::class.java)
            assertEquals(c, constraint)
        }
    }

    @Test
    fun testConstraintDeserializationWithInvalidUri() {
        assertThrows<InvalidFormatException> {
            mapper.readValue(
                "{\n" +
                        "  \"@context\": \"http://www.w3.org/ns/odrl.jsonld\",\n" +
                        "  \"@type\": \"Constraint\",\n" +
                        "  \"@id\": \":::invalid-uri:::\"\n" +
                        "}", OdrlConstraint::class.java
            )
        }
    }
}