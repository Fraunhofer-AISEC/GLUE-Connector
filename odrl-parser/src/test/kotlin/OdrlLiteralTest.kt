import fhg.glue.odrl.GlueContainer
import fhg.glue.odrl.GlueContainerPipeline
import fhg.glue.odrl.OdrlLiteral
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.Test

class OdrlLiteralTest {
    @Test
    fun equality() {
        assertEquals(OdrlLiteral("test"), OdrlLiteral("test"))
        assertEquals(OdrlLiteral(42), OdrlLiteral(42))
        assertEquals(OdrlLiteral(true), OdrlLiteral(true))
        assertEquals(OdrlLiteral(42.4), OdrlLiteral(42.4))
        val datetime = LocalDateTime.now()
        assertEquals(OdrlLiteral(datetime), OdrlLiteral(datetime))
        val stringList = listOf("one", "two", "three")
        assertEquals(OdrlLiteral(stringList), OdrlLiteral(stringList))
        val intList = listOf(1, 2, 3)
        assertEquals(OdrlLiteral(intList), OdrlLiteral(intList))
        assertEquals(OdrlLiteral("test"), OdrlLiteral("test"))
    }

    @Test
    fun containerPipelineEquality() {
        val list1 = GlueContainerPipeline(listOf(GlueContainer("image1", "hash1",), GlueContainer("image2", "hash2",)))
        val list2 = GlueContainerPipeline(listOf(GlueContainer("image2", "hash2",), GlueContainer("image1", "hash1",)))
        val list3 = GlueContainerPipeline(listOf(GlueContainer("image1", "hash1",), GlueContainer("image2", "hash2",)))
        assertEquals(OdrlLiteral(list1), OdrlLiteral(list1))
        assertEquals(OdrlLiteral(list1), OdrlLiteral(list3))
        assertEquals(OdrlLiteral(list3), OdrlLiteral(list1))
        assertNotEquals(OdrlLiteral(list1), OdrlLiteral(list2))
        assertNotEquals(OdrlLiteral(list2), OdrlLiteral(list1))
    }

    @Test
    fun comparison() {
        val datetime = LocalDateTime.now()
        assertTrue(OdrlLiteral(42) <= OdrlLiteral(100))
        assertTrue(OdrlLiteral(42) > OdrlLiteral(-1))
        assertTrue(OdrlLiteral(42.4) <= OdrlLiteral(100.4))
        assertTrue(OdrlLiteral(42.4) > OdrlLiteral(0))
        assertTrue(OdrlLiteral(datetime) <= OdrlLiteral(LocalDateTime.now()))
    }

    @Test
    fun crossTypeEquality() {
        assertTrue(OdrlLiteral(42) == OdrlLiteral("42"))
        assertTrue(OdrlLiteral("-14") == OdrlLiteral(-14))
        assertTrue(OdrlLiteral(42.4) == OdrlLiteral("42.4"))
        assertTrue(OdrlLiteral("-2.1") == OdrlLiteral(-2.1))
        assertTrue(OdrlLiteral(true) == OdrlLiteral("true"))
        assertTrue(OdrlLiteral("false") == OdrlLiteral(false))
        assertTrue(OdrlLiteral(true) == OdrlLiteral(1))
        assertTrue(OdrlLiteral(0) == OdrlLiteral(false))
    }

    @Test
    fun listComparison() {
        val stringList = listOf("one", "two", "three")
        assertTrue(OdrlLiteral(stringList).hasPart(OdrlLiteral("two")))
        assertFalse(OdrlLiteral(stringList).hasPart(OdrlLiteral("four")))
        assertTrue(OdrlLiteral("one").isPartOf(OdrlLiteral(stringList)))
        assertFalse(OdrlLiteral("six").isPartOf(OdrlLiteral(stringList)))

        val intList = listOf(1, 2, 3)
        assertTrue(OdrlLiteral(intList).hasPart(OdrlLiteral(2)))
        assertFalse(OdrlLiteral(intList).hasPart(OdrlLiteral(4)))
        assertTrue(OdrlLiteral(1).isPartOf(OdrlLiteral(intList)))
        assertFalse(OdrlLiteral(6).isPartOf(OdrlLiteral(intList)))
    }
}