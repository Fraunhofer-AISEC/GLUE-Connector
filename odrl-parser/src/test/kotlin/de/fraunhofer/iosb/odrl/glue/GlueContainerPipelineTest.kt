package de.fraunhofer.iosb.odrl.glue

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.JsonNode
import de.fraunhofer.iosb.odrl.*
import de.fraunhofer.iosb.odrl.Commons.Companion.loadFileOrDie
import org.junit.jupiter.api.assertThrows
import java.io.StringReader
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class GlueContainerPipelineTest {
    private val mapper = Commons.parserInstance.mapper
    private val pipelines = listOf(
        GlueContainerPipeline(
            platformSecurity = listOf("tpm2", "sgx"),
            container = listOf(
                GlueContainer(
                    image = "test/container1",
                    hash = "364a4787748a98c1c28085d4154a06c3a9be9a67c0b08a56b3f69fa8fabf172e",
                    allowNetwork = true,
                    parameter = listOf(GlueRestParameter(
                        httpType = GlueHttpType.GET,
                        httpUrl = URI.create("http://0.0.0.0:1234/dockerEndpoint?key1=value1")
                    ))
                ),
                GlueContainer(
                    image = "test/container2",
                    hash = "ac924fdd26993b2ad157e3512c4a36ad293491f6eb8d268d6d4e1bf1c5c307ad",
                    allowNetwork = false
                )
            )
        ),
        GlueContainerPipeline(
            container = listOf(
                GlueContainer(
                    image = "test/container1",
                    hash = "364a4787748a98c1c28085d4154a06c3a9be9a67c0b08a56b3f69fa8fabf172e",
                    allowNetwork = true,
                    parameter = listOf(GlueRestParameter(
                        httpType = GlueHttpType.GET,
                        httpUrl = URI.create("http://0.0.0.0:1234/dockerEndpoint?key1=value1")
                    ))
                ),
                GlueContainer(
                    image = "test/container2",
                    hash = "ac924fdd26993b2ad157e3512c4a36ad293491f6eb8d268d6d4e1bf1c5c307ad",
                    allowNetwork = false,
                    parameter = listOf(GlueRestParameter(
                        httpType = GlueHttpType.POST,
                        httpUrl = URI.create("http://0.0.0.0:1234/dockerEndpoint"),
                        value = GlueValue(mapper.readValue(
                        "{\n" +
                                "  \"http://example.com/valueProperty1\": \"testProperty\",\n" +
                                "  \"http://example.com/valueProperty2\": [42, 43, 44]\n" +
                                "}", JsonNode::class.java)
                        )
                    ))
                )
            )
        )
    )

    @Test
    fun testGlueContainerPipelineEquality() {
        val pipeline = GlueContainerPipeline(
            platformSecurity = listOf("tpm2", "sgx"),
            container = listOf(
                GlueContainer(
                    image = "test/container1",
                    hash = "364a4787748a98c1c28085d4154a06c3a9be9a67c0b08a56b3f69fa8fabf172e",
                    allowNetwork = true,
                    parameter = listOf(GlueRestParameter(
                        httpType = GlueHttpType.GET,
                        httpUrl = URI.create("http://0.0.0.0:1234/dockerEndpoint?key1=value1")
                    ))
                ),
                GlueContainer(
                    image = "test/container2",
                    hash = "ac924fdd26993b2ad157e3512c4a36ad293491f6eb8d268d6d4e1bf1c5c307ad",
                    allowNetwork = false
                )
            )
        )
        assertEquals(pipeline, pipelines[0]);
        assertNotEquals(pipeline, pipelines[1]);
    }

    @Test
    fun testGlueContainerPipelineComparison() {
        assertThrows<UnsupportedOperationException> {
            pipelines[0] <= pipelines[1]
        }
    }

    @Test
    fun testGlueContainerPipelineDeserialization() {
        pipelines.forEachIndexed { i, p ->
            val pipeline = mapper.readValue(
                loadFileOrDie("/glue-container-pipeline-example-${i+1}.json"),
                GlueContainerPipeline::class.java
            )
            assertEquals(p, pipeline)
        }
    }

    @Test
    fun testGlueContainerPipelineSerializationAndDeserialization() {
        pipelines.forEach { p ->
            val string = mapper.writeValueAsString(p)
            val pipeline = mapper.readValue(StringReader(string), GlueContainerPipeline::class.java)
            assertEquals(p, pipeline)
        }
    }

    @Test
    fun testGlueContainerPipelineWithNoContainers() {
        assertThrows<JsonMappingException> {
            mapper.readValue(
            "{\n" +
                    "  \"@context\": {},\n" +
                    "  \"@type\": \"http://fhg.de/glue/ContainerPipeline\",\n" +
                    "  \"http://fhg.de/glue/container\": []\n" +
                    "}", GlueContainerPipeline::class.java
            )
        }
    }

    @Test
    fun testGlueContainerPipelineWithInvalidPlatformSecurity() {
        assertThrows<JsonMappingException> {
            mapper.readValue(
                "{\n" +
                        "  \"@context\": {},\n" +
                        "  \"@type\": \"http://fhg.de/glue/ContainerPipeline\",\n" +
                        "  \"http://fhg.de/glue/platformSecurity\": [\"tpm\"],\n" +
                        "  \"http://fhg.de/glue/container\": {\n" +
                        "    \"@type\": \"http://fhg.de/glue/Container\",\n" +
                        "    \"http://fhg.de/glue/image\": \"test/container1\",\n" +
                        "    \"http://fhg.de/glue/hash\": \"364a4787748a98c1c28085d4154a06c3a9be9a67c0b08a56b3f69fa8fabf172e\"\n" +
                        "  }\n" +
                        "}", GlueContainerPipeline::class.java
            )
        }
    }
}