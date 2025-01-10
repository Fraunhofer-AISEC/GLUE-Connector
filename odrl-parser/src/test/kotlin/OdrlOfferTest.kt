import de.fraunhofer.iosb.odrl.*
import de.fraunhofer.iosb.odrl.parser.OdrlParser
import java.io.StringReader
import java.net.URI
import java.time.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class OdrlOfferTest {
    private lateinit var parser: OdrlParser
    private lateinit var offer1: OdrlOffer

    @BeforeTest
    fun setup() {
        parser = OdrlParser()
        offer1 = OdrlOffer(
            uid = URI.create("http://example.com/policy"),
            profile = listOf(OdrlIri(URI.create("http://example.com/profile"))),
            assigner = OdrlParty(URI.create("http://example.com/assigner")),
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
    fun testOfferDeserialization() {
        val offer = parser.readOdrlPolicy(loadFileOrDie("/offer-example-1.json"))
        assertEquals(offer1, offer)
    }

    @Test
    fun testOfferSerializationAndDeserialization() {
        val string = parser.writeOdrlPolicy(offer1)
        val offer = parser.readOdrlPolicy(StringReader(string))
        assertEquals(offer1, offer)
    }

}