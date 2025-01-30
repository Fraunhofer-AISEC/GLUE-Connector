package de.fraunhofer.iosb.odrl.glue

import com.fasterxml.jackson.databind.JsonNode
import de.fraunhofer.iosb.odrl.*
import de.fraunhofer.iosb.odrl.Commons.Companion.loadFileOrDie
import java.io.StringReader
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals

class GlueRestParameterTest {
    private val mapper = Commons.parserInstance.mapper
    private val restParameter1 = GlueRestParameter(
        httpType = GlueHttpType.POST,
        httpUrl = URI.create("http://0.0.0.0:1234/dockerEndpoint"),
        value = GlueValue(mapper.readValue(
        "{\n" +
                "  \"@type\": \"http://www.w3.org/ns/odrl/2/Policy\",\n" +
                "  \"@id\": \"http://example.com/policy\",\n" +
                "  \"http://www.w3.org/ns/odrl/2/permission\": {\n" +
                "    \"http://www.w3.org/ns/odrl/2/target\": {\n" +
                "      \"@id\": \"http://dataowner.com/document:1234\"\n" +
                "    },\n" +
                "    \"http://www.w3.org/ns/odrl/2/action\": {\n" +
                "      \"@id\": \"http://www.w3.org/ns/odrl/2/use\"\n" +
                "    },\n" +
                "    \"http://www.w3.org/ns/odrl/2/constraint\": {\n" +
                "      \"@type\": \"http://fhg.de/glue/ParameterPlaceholder\",\n" +
                "      \"http://fhg.de/glue/key\": \"nextDataReceiverConstraint\"\n" +
                "    }\n" +
                "  }\n" +
                "}", JsonNode::class.java)
        )
    )

    @Test
    fun testGlueRestParameterDeserialization() {
        val parameter =
            mapper.readValue(loadFileOrDie("/glue-rest-parameter-example-1.json"), GlueRestParameter::class.java)
        assertEquals(restParameter1, parameter)
    }

    @Test
    fun testGlueContainerPipelineSerializationAndDeserialization() {
        val string = mapper.writeValueAsString(restParameter1)
        val parameter = mapper.readValue(StringReader(string), GlueRestParameter::class.java)
        assertEquals(restParameter1, parameter)
    }
}