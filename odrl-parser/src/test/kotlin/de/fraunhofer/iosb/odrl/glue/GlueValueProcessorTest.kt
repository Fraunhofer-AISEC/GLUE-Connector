/*
 * Copyright 2024-2025 Fraunhofer AISEC and Fraunhofer IOSB
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
import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.TextNode
import de.fraunhofer.iosb.odrl.*
import de.fraunhofer.iosb.odrl.de.fraunhofer.iosb.odrl.glue.GlueParameterPlaceholderCallback
import de.fraunhofer.iosb.odrl.parser.OdrlParser
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class GlueValueProcessorTest {
    private val mapper = OdrlParser().mapper
    private val processor = GlueValueProcessor()
    private val callback =
        GlueParameterPlaceholderCallback {
            when (it) {
                "testKeyString" -> TextNode.valueOf("testValue")
                "testKeyBool" -> BooleanNode.valueOf(true)
                "nextDataReceiverUid" -> getTestNextDataReceiverUid()
                "nextDataReceiverConstraint" -> getTestNextDataReceiverConstraint()
                else -> NullNode.getInstance()
            }
        }

    private fun getTestNextDataReceiverUid(): TextNode = TextNode.valueOf("http://nextdataowner.com/")

    private fun getTestNextDataReceiverConstraint(): JsonNode =
        mapper.convertValue(
            OdrlConstraint(
                null,
                OdrlLeftOperand("dateTime"),
                OdrlOperator("lt"),
                listOf(XmlDate(LocalDate.of(2035, 12, 31))),
            ),
            JsonNode::class.java,
        )

    @Test
    fun testPrimitiveParameterReplacement() {
        val v = GlueValue(TextNode.valueOf("http://fhg.de/glue/ParameterPlaceholder?key=testKeyString"))
        runBlocking {
            assertEquals(GlueValue(TextNode.valueOf("testValue")), processor.resolvePlaceholders(v, callback))
        }
    }

    @Test
    fun testComplexParameterReplacement() {
        val v = GlueValue(mapper.convertValue(GlueParameterPlaceholder("testKeyBool"), JsonNode::class.java))
        runBlocking {
            assertEquals(GlueValue(BooleanNode.valueOf(true)), processor.resolvePlaceholders(v, callback))
        }
    }

    @Test
    fun testGlueRestParameterReplacement() {
        val s =
            "{\n" +
                "  \"@type\": \"http://fhg.de/glue/RestParameter\",\n" +
                "  \"http://fhg.de/glue/httpUrl\": \"http://0.0.0.0:1234/dockerEndpoint\",\n" +
                "  \"http://fhg.de/glue/httpType\": \"POST\",\n" +
                "  \"http://fhg.de/glue/value\": {\n" +
                "    \"@type\": \"http://fhg.de/glue/Value\",\n" +
                "    \"http://fhg.de/glue/json\": {\n" +
                "      \"@context\": \"http://www.w3.org/ns/odrl.jsonld\",\n" +
                "      \"@type\": \"Policy\",\n" +
                "      \"uid\": \"http://dataowner.com/policy:987\",\n" +
                "      \"permission\": [\n" +
                "        {\n" +
                "          \"target\": \"http://dataowner.com/document:1234\",\n" +
                "          \"assigner\": \"http://dataconsumer.com/org\",\n" +
                "          \"assignee\": \"http://fhg.de/glue/ParameterPlaceholder?key=nextDataReceiverUid\",\n" +
                "          \"action\": \"use\",\n" +
                "          \"constraint\": {\n" +
                "            \"@type\": \"http://fhg.de/glue/ParameterPlaceholder\",\n" +
                "            \"http://fhg.de/glue/key\": \"nextDataReceiverConstraint\"\n" +
                "          }\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}"

        val param = mapper.readValue(s, GlueRestParameter::class.java)
        runBlocking {
            val v = processor.resolvePlaceholders(param.value!!, callback)
            val nextDataReceiverUid = v.json.findValue(OdrlIdentifiers.ASSIGNEE).get("@id")
            val nextDataReceiverConstraint = v.json.findValue(OdrlIdentifiers.HAS_CONSTRAINT)
            assertEquals(getTestNextDataReceiverUid(), nextDataReceiverUid)
            assertEquals(getTestNextDataReceiverConstraint(), nextDataReceiverConstraint)
        }
    }
}
