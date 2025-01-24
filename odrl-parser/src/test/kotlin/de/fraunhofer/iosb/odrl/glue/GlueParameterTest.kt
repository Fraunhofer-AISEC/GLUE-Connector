package de.fraunhofer.iosb.odrl.glue

import com.fasterxml.jackson.databind.JsonNode
import de.fraunhofer.iosb.odrl.*
import java.io.StringReader
import kotlin.test.Test
import kotlin.test.assertEquals

class GlueParameterTest {
    private val mapper = Commons.parserInstance.mapper
    private val parameter1 = GlueParameter(
        value = GlueValue.create(mapper.readValue("[\"testValue1\", \"testValue2\"]", JsonNode::class.java))
    )
    private val parameter1Json = "{\n" +
            "  \"@context\": {},\n" +
            "  \"@type\": \"http://fhg.de/glue/Parameter\",\n" +
            "  \"http://fhg.de/glue/value\": [\"testValue1\", \"testValue2\"]\n" +
            "}"

    @Test
    fun testGlueParameterDeserialization() {
        val parameter = mapper.readValue(parameter1Json, GlueParameter::class.java)
        assertEquals(parameter1, parameter)
    }

    @Test
    fun testGlueContainerPipelineSerializationAndDeserialization() {
        val string = mapper.writeValueAsString(parameter1)
        val parameter = mapper.readValue(StringReader(string), GlueParameter::class.java)
        assertEquals(parameter1, parameter)
    }
}