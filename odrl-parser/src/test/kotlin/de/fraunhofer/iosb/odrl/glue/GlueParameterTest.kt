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

import com.fasterxml.jackson.databind.JsonNode
import de.fraunhofer.iosb.odrl.*
import java.io.StringReader
import kotlin.test.Test
import kotlin.test.assertEquals

class GlueParameterTest {
    private val mapper = Commons.parserInstance.mapper
    private val parameter1 =
        GlueParameter(
            value = GlueValue(mapper.readValue("[\"testValue1\", \"testValue2\"]", JsonNode::class.java)),
        )
    private val parameter1Json =
        "{\n" +
            "  \"@context\": {},\n" +
            "  \"@type\": \"http://fhg.de/glue/Parameter\",\n" +
            "  \"http://fhg.de/glue/value\": {\n" +
            "    \"@type\": \"http://fhg.de/glue/Value\",\n" +
            "   \"http://fhg.de/glue/json\": [\"testValue1\", \"testValue2\"]\n" +
            "  }\n" +
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
