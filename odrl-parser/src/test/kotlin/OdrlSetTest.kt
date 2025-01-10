import de.fraunhofer.iosb.odrl.*
import de.fraunhofer.iosb.odrl.parser.OdrlParser
import java.io.StringReader
import java.net.URI
import java.time.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class OdrlSetTest {
    private lateinit var parser: OdrlParser
    private lateinit var set1: OdrlSet

    @BeforeTest
    fun setup() {
        parser = OdrlParser()
        set1 = OdrlSet(
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
    }

    @Test
    fun testSetDeserialization() {
        val set = parser.readOdrlPolicy(loadFileOrDie("/set-example-1.json"))
        assertEquals(set1, set)
    }

    @Test
    fun testSetSerializationAndDeserialization() {
        val string = parser.writeOdrlPolicy(set1)
        val set = parser.readOdrlPolicy(StringReader(string))
        assertEquals(set1, set)
    }

}