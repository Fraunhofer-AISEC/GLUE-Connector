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
    private val callback = GlueParameterPlaceholderCallback {
        when (it) {
            "testKeyString" -> TextNode.valueOf("testValue")
            "testKeyBool" -> BooleanNode.valueOf(true)
            "nextDataReceiverUid" -> getTestNextDataReceiverUid()
            "nextDataReceiverConstraint" -> getTestNextDataReceiverConstraint()
            else -> NullNode.getInstance()
        }
    }

    private fun getTestNextDataReceiverUid(): TextNode {
        return TextNode.valueOf("http://nextdataowner.com/")
    }

    private fun getTestNextDataReceiverConstraint(): JsonNode {
        return mapper.convertValue(
            OdrlConstraint(
                null,
                OdrlLeftOperand("dateTime"),
                OdrlOperator("lt"),
                listOf(XmlDate(LocalDate.of(2035, 12, 31)))
            ),
            JsonNode::class.java
        )
    }

    @Test
    fun testPrimitiveParameterReplacement() {
        val v = GlueValue.create(TextNode.valueOf("http://fhg.de/glue/ParameterPlaceholder?key=testKeyString"))
        runBlocking {
            assertEquals(GlueValue.create(TextNode.valueOf("testValue")), processor.resolvePlaceholders(v, callback))
        }
    }

    @Test
    fun testComplexParameterReplacement() {
        val v = GlueValue.create(mapper.convertValue(GlueParameterPlaceholder("testKeyBool"), JsonNode::class.java))
        runBlocking {
            assertEquals(GlueValue.create(BooleanNode.valueOf(true)), processor.resolvePlaceholders(v, callback))
        }
    }

    @Test
    fun testGlueRestParameterReplacement() {
        val s = "{\n" +
                "  \"@type\": \"http://fhg.de/glue/RestParameter\",\n" +
                "  \"http://fhg.de/glue/httpUrl\": \"http://0.0.0.0:1234/dockerEndpoint\",\n" +
                "  \"http://fhg.de/glue/httpType\": \"POST\",\n" +
                "  \"http://fhg.de/glue/value\": {\n" +
                "    \"@context\": \"http://www.w3.org/ns/odrl.jsonld\",\n" +
                "    \"@type\": \"Policy\",\n" +
                "    \"uid\": \"http://dataowner.com/policy:987\",\n" +
                "    \"permission\": [\n" +
                "      {\n" +
                "        \"target\": \"http://dataowner.com/document:1234\",\n" +
                "        \"assigner\": \"http://dataconsumer.com/org\",\n" +
                "        \"assignee\": \"http://fhg.de/glue/ParameterPlaceholder?key=nextDataReceiverUid\",\n" +
                "        \"action\": \"use\",\n" +
                "        \"constraint\": {\n" +
                "          \"@type\": \"http://fhg.de/glue/ParameterPlaceholder\",\n" +
                "          \"http://fhg.de/glue/key\": \"nextDataReceiverConstraint\"\n" +
                "        }\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}"

        val param = mapper.readValue(s, GlueRestParameter::class.java)
        runBlocking {
            val v = processor.resolvePlaceholders(param.value!!, callback)
            val nextDataReceiverUid = v.jsonNode.findValue(OdrlIdentifiers.ASSIGNEE).get("@id")
            val nextDataReceiverConstraint = v.jsonNode.findValue(OdrlIdentifiers.HAS_CONSTRAINT)
            assertEquals(getTestNextDataReceiverUid(), nextDataReceiverUid)
            assertEquals(getTestNextDataReceiverConstraint(), nextDataReceiverConstraint)
        }
    }
}