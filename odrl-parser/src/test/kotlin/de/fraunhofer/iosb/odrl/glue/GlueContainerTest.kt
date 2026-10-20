/*
 * Copyright 2025 Fraunhofer AISEC and Fraunhofer IOSB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.fraunhofer.iosb.odrl.glue

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import de.fraunhofer.iosb.odrl.*
import de.fraunhofer.iosb.odrl.Commons.Companion.loadFileOrDie
import org.junit.jupiter.api.assertThrows
import java.io.StringReader
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals

class GlueContainerTest {
    private val mapper = Commons.parserInstance.mapper
    private val container1 =
        GlueContainer(
            image = "test/container1",
            hash = "364a4787748a98c1c28085d4154a06c3a9be9a67c0b08a56b3f69fa8fabf172e",
            allowNetwork = true,
            parameter =
                listOf(
                    GlueRestParameter(
                        httpType = GlueHttpType.GET,
                        httpUrl = URI.create("http://0.0.0.0:1234/dockerEndpoint?key1=value1"),
                    ),
                ),
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
        assertThrows<JsonMappingException> {
            mapper.readValue(
                "{\n" +
                    "  \"@context\": {},\n" +
                    "  \"@type\": \"http://fhg.de/glue/Container\",\n" +
                    "  \"http://fhg.de/glue/image\": \"\",\n" +
                    "  \"http://fhg.de/glue/hash\": \"364a4787748a98c1c28085d4154a06c3a9be9a67c0b08a56b3f69fa8fabf172e\"\n" +
                    "}",
                GlueContainer::class.java,
            )
        }
        assertThrows<JsonMappingException> {
            mapper.readValue(
                "{\n" +
                    "  \"@context\": {},\n" +
                    "  \"@type\": \"http://fhg.de/glue/Container\",\n" +
                    "  \"http://fhg.de/glue/image\": \"test/container1\",\n" +
                    "  \"http://fhg.de/glue/hash\": \"\"\n" +
                    "}",
                GlueContainer::class.java,
            )
        }
    }
}
