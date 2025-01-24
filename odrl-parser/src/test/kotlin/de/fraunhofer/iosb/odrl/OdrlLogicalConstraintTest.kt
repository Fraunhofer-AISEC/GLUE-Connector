package de.fraunhofer.iosb.odrl

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import de.fraunhofer.iosb.odrl.Commons.Companion.loadFileOrDie
import org.junit.jupiter.api.assertThrows
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals

class OdrlLogicalConstraintTest {
    private val mapper = Commons.parserInstance.mapper
    private val constraints = listOf(
        OdrlLogicalConstraint(
            uid = URI.create("http://example.com/logicalConstraint"),
            or = listOf(
                OdrlConstraint(
                    leftOperand = OdrlLeftOperand("http://example.com/leftOperand1"),
                    operator = OdrlOperator("http://www.w3.org/ns/odrl/2/eq"),
                    rightOperand = listOf(PrimitiveString("exampleRightOperand1")),
                ),
                OdrlConstraint(
                    leftOperand = OdrlLeftOperand("http://example.com/leftOperand2"),
                    operator = OdrlOperator("http://www.w3.org/ns/odrl/2/eq"),
                    rightOperand = listOf(PrimitiveString("exampleRightOperand2")),
                )
            )
        ),
        OdrlLogicalConstraint(
            uid = URI.create("http://example.com/logicalConstraint"),
            xone = listOf(
                OdrlConstraint(
                    leftOperand = OdrlLeftOperand("http://example.com/leftOperand1"),
                    operator = OdrlOperator("http://www.w3.org/ns/odrl/2/eq"),
                    rightOperand = listOf(PrimitiveString("exampleRightOperand1")),
                ),
                OdrlConstraint(
                    leftOperand = OdrlLeftOperand("http://example.com/leftOperand2"),
                    operator = OdrlOperator("http://www.w3.org/ns/odrl/2/eq"),
                    rightOperand = listOf(PrimitiveString("exampleRightOperand2")),
                )
            )
        ),
        OdrlLogicalConstraint(
            uid = URI.create("http://example.com/logicalConstraint"),
            and = listOf(
                OdrlConstraint(
                    leftOperand = OdrlLeftOperand("http://example.com/leftOperand1"),
                    operator = OdrlOperator("http://www.w3.org/ns/odrl/2/eq"),
                    rightOperand = listOf(PrimitiveString("exampleRightOperand1")),
                ),
                OdrlConstraint(
                    leftOperand = OdrlLeftOperand("http://example.com/leftOperand2"),
                    operator = OdrlOperator("http://www.w3.org/ns/odrl/2/eq"),
                    rightOperand = listOf(PrimitiveString("exampleRightOperand2")),
                )
            )
        ),
        OdrlLogicalConstraint(
            uid = URI.create("http://example.com/logicalConstraint"),
            andSequence = listOf(
                OdrlConstraint(
                    leftOperand = OdrlLeftOperand("http://example.com/leftOperand1"),
                    operator = OdrlOperator("http://www.w3.org/ns/odrl/2/eq"),
                    rightOperand = listOf(PrimitiveString("exampleRightOperand1")),
                ),
                OdrlConstraint(
                    leftOperand = OdrlLeftOperand("http://example.com/leftOperand2"),
                    operator = OdrlOperator("http://www.w3.org/ns/odrl/2/eq"),
                    rightOperand = listOf(PrimitiveString("exampleRightOperand2")),
                )
            )
        )
    )

    @Test
    fun testLogicalConstraintDeserialization() {
        constraints.forEachIndexed { i, c ->
            val constraint = mapper.readValue(
                loadFileOrDie("/logical-constraint-example-${i + 1}.json"),
                OdrlLogicalConstraint::class.java
            )
            assertEquals(c, constraint)
        }
    }

    @Test
    fun testLogicalConstraintSerializationAndDeserialization() {
        constraints.forEach { c ->
            val string = mapper.writeValueAsString(c)
            val constraint = mapper.readValue(string, OdrlLogicalConstraint::class.java)
            assertEquals(c, constraint)
        }
    }

    @Test
    fun testLogicalConstraintDeserializationWithInvalidUri() {
        assertThrows<InvalidFormatException> {
            mapper.readValue(
                "{\n" +
                        "  \"@context\": \"http://www.w3.org/ns/odrl.jsonld\",\n" +
                        "  \"@type\": \"LogicalConstraint\",\n" +
                        "  \"@id\": \":::invalid-uri:::\"\n" +
                        "}", OdrlLogicalConstraint::class.java
            )
        }
    }
}