package de.fraunhofer.iosb.odrl.glue

import com.fasterxml.jackson.core.JsonParseException
import de.fraunhofer.iosb.odrl.*
import de.fraunhofer.iosb.odrl.Commons.Companion.loadFileOrDie
import org.junit.jupiter.api.assertThrows
import java.io.StringReader
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals

class GlueContainerTest {
    private val mapper = Commons.parserInstance.mapper
    private val container1 = GlueContainer(
        image = "test/container1",
        hash = "364a4787748a98c1c28085d4154a06c3a9be9a67c0b08a56b3f69fa8fabf172e",
        allowNetwork = true,
        parameter = listOf(GlueRestParameter(
            httpType = GlueHttpType.GET,
            httpUrl = URI.create("http://0.0.0.0:1234/dockerEndpoint?key1=value1")
        ))
    )

    @Test
    fun testGlueContainerDeserialization() {
        val container = mapper.readValue(loadFileOrDie("/glue-container-example-1.json"), GlueContainer::class.java)
        assertEquals(container1, container)
    }

    @Test
    fun testGlueContainerPipelineSerializationAndDeserialization() {
        val string = mapper.writeValueAsString(container1)
        val container = mapper.readValue(StringReader(string), GlueContainer::class.java)
        assertEquals(container1, container)
    }

    @Test
    fun testGlueContainerWithEmptyImageAndHash() {
        assertThrows<JsonParseException> {
            mapper.readValue(
            "{\n" +
                    "  \"@context\": {},\n" +
                    "  \"@type\": \"http://fhg.de/glue/Container\",\n" +
                    "  \"image\": \"\"\n" +
                    "  \"hash\": \"364a4787748a98c1c28085d4154a06c3a9be9a67c0b08a56b3f69fa8fabf172e\"\n" +
                    "}", GlueContainer::class.java
            )
        }
        assertThrows<JsonParseException> {
            mapper.readValue(
                "{\n" +
                        "  \"@context\": {},\n" +
                        "  \"@type\": \"http://fhg.de/glue/Container\",\n" +
                        "  \"image\": \"test/container1\"\n" +
                        "  \"hash\": \"\"\n" +
                        "}", GlueContainer::class.java
            )
        }
    }
}