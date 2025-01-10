import de.fraunhofer.iosb.odrl.*
import de.fraunhofer.iosb.odrl.parser.OdrlParser
import java.io.StringReader
import java.net.URI
import java.time.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class OdrlAgreementTest {
    private lateinit var parser: OdrlParser
    private lateinit var agreement1: OdrlAgreement

    @BeforeTest
    fun setup() {
        parser = OdrlParser()
        agreement1 = OdrlAgreement(
            uid = URI.create("http://example.com/policy"),
            profile = listOf(OdrlIri(URI.create("http://example.com/profile"))),
            assigner = OdrlParty(URI.create("http://example.com/assigner")),
            assignee = OdrlParty(URI.create("http://example.com/assignee")),
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
    fun testAgreementDeserialization() {
        val agreement = parser.readOdrlPolicy(loadFileOrDie("/agreement-example-1.json"))
        assertEquals(agreement1, agreement)
    }

    @Test
    fun testAgreementSerializationAndDeserialization() {
        val string = parser.writeOdrlPolicy(agreement1)
        val agreement = parser.readOdrlPolicy(StringReader(string))
        assertEquals(agreement1, agreement)
    }

}