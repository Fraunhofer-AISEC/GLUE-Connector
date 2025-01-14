import de.fraunhofer.iosb.odrl.*
import java.io.StringReader
import java.net.URI
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class OdrlPolicyTest {
    private val parser = Commons.parserInstance
    private val policy1 = OdrlPolicy(
        uid = URI.create("http://example.com/policy"),
        profile = listOf(OdrlIri(URI.create("http://example.com/profile"))),
        permission = listOf(OdrlPermission(
            uid = URI.create("http://example.com/permission"),
            target = OdrlAsset(URI.create("http://example.com/target")),
            action = OdrlAction("http://www.w3.org/ns/odrl/2/play"),
            constraint = listOf(OdrlConstraint(
                leftOperand = OdrlLeftOperand(URI.create("http://www.w3.org/ns/odrl/2/dateTime")),
                operator = OdrlOperator(URI.create("http://www.w3.org/ns/odrl/2/lteq")),
                rightOperand = listOf(XmlDate(LocalDate.of(2045, 12, 31)))
            ))
        ))
    )

    @Test
    fun testPolicyDeserialization() {
        val policy = parser.readOdrlPolicy(loadFileOrDie("/policy-example-1.json"))
        assertEquals(policy1, policy)
    }

    @Test
    fun testPolicySerializationAndDeserialization() {
        val string = parser.writeOdrlPolicy(policy1)
        val policy = parser.readOdrlPolicy(StringReader(string))
        assertEquals(policy1, policy)
    }
}