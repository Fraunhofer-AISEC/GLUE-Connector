import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import de.fraunhofer.iosb.odrl.*
import de.fraunhofer.iosb.odrl.de.fraunhofer.iosb.odrl.glue.GlueParameterPlaceholderCallback
import de.fraunhofer.iosb.odrl.glue.*
import de.fraunhofer.iosb.odrl.parser.OdrlParser
import jakarta.json.Json
import jakarta.json.JsonValue
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GlueValueProcessorTest {
    private lateinit var mapper: ObjectMapper
    private lateinit var processor: GlueValueProcessor
    private lateinit var callback: GlueParameterPlaceholderCallback

    @BeforeTest
    fun setup() {
        mapper = OdrlParser().mapper;
        processor = GlueValueProcessor()
        callback = GlueParameterPlaceholderCallback {
            when (it) {
                "testKeyString" -> Json.createValue("testValue")
                "testKeyBool" -> JsonValue.TRUE
                "nextDataReceiverUid" -> getTestNextDataReceiverUid()
                "nextDataReceiverConstraint" -> getTestNextDataReceiverConstraint()
                else -> JsonValue.NULL
            }
        }
    }

    private fun getTestNextDataReceiverUid(): JsonValue {
        return Json.createValue("http://nextdataowner.com/")
    }

    private fun getTestNextDataReceiverConstraint(): JsonValue {
        val s = mapper.writeValueAsString(
            OdrlConstraint(
                null,
                OdrlLeftOperand("dateTime"),
                OdrlOperator("lt"),
                listOf(XmlDate(LocalDate.of(2035, 12, 31)))
            )
        )
        val typeRef = object : TypeReference<HashMap<String, Any?>>() {}
        return Json.createObjectBuilder(mapper.readValue(s, typeRef)).build()
    }

    @Test
    fun testPrimitiveParameterReplacement() {
        val v = GlueValue.create("http://fhg.de/glue/ParameterPlaceholder?key=testKeyString")
        runBlocking {
            assertEquals(GlueValue.create("testValue"), processor.resolvePlaceholders(v, callback))
        }
    }

    @Test
    fun testComplexParameterReplacement() {
        val v = GlueValue.create(mapOf(Pair("@type", GlueIdentifiers.PARAMETER_PLACEHOLDER), Pair(GlueIdentifiers.KEY, "testKeyBool")))
        runBlocking {
            assertEquals(GlueValue.create(true), processor.resolvePlaceholders(v, callback))
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
            val permission = v.jsonValue.asJsonObject()[OdrlIdentifiers.HAS_PERMISSION]!!.asJsonObject()
            val nextDataReceiverUid = permission[OdrlIdentifiers.ASSIGNEE]!!.asJsonObject()["@id"]
            val nextDataReceiverConstraint = permission[OdrlIdentifiers.HAS_CONSTRAINT]
            assertEquals(getTestNextDataReceiverUid(), nextDataReceiverUid)
            assertEquals(getTestNextDataReceiverConstraint(), nextDataReceiverConstraint)
        }
    }

}