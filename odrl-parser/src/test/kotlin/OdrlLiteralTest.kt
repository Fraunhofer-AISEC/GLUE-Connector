import fhg.glue.odrl.de.fraunhofer.iosb.odrl.*
import org.junit.jupiter.api.Assertions.assertFalse
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.Test
import kotlin.test.assertTrue

class OdrlLiteralTest {
    @Test
    fun equality() {
        assertEquals(PrimitiveBoolean(false), PrimitiveBoolean(false))
        assertEquals(PrimitiveInt(42), PrimitiveInt(42))
        assertEquals(PrimitiveDouble(42.42), PrimitiveDouble(42.42))
        assertEquals(PrimitiveString("test"), PrimitiveString("test"))

        assertEquals(XmlBoolean(true), XmlBoolean(true))
        assertEquals<OdrlRightOperand>(XmlBoolean(true), PrimitiveBoolean(true))

        assertEquals(XmlByte(42), XmlByte(42))
        assertEquals<OdrlRightOperand>(PrimitiveInt(42), XmlByte(42))

        assertEquals(XmlInt(42), XmlInt(42))
        assertEquals<OdrlRightOperand>(PrimitiveInt(42), XmlInt(42))

        assertEquals(XmlInteger(42), XmlInteger(42))
        assertEquals<OdrlRightOperand>(PrimitiveInt(42), XmlInteger(42))

        assertEquals(XmlLong(42L), XmlLong(42L))
        assertEquals<OdrlRightOperand>(XmlLong(42L), PrimitiveInt(42))

        assertEquals(XmlDouble(42.42), XmlDouble(42.42))
        assertEquals<OdrlRightOperand>(XmlDouble(42.42), PrimitiveDouble(42.42))

        assertEquals(XmlString("test"), XmlString("test"))
        assertEquals<OdrlRightOperand>(XmlString("test"), PrimitiveString("test"))

        val datetime = LocalDateTime.now()
        assertEquals(XmlDateTime(datetime), XmlDateTime(datetime))
        assertEquals(XmlDate(datetime.toLocalDate()), XmlDate(datetime.toLocalDate()))
        assertEquals(XmlTime(datetime.toLocalTime()), XmlTime(datetime.toLocalTime()))
    }

    @Test
    fun comparison() {
        val datetime = LocalDateTime.now()
        assertTrue(PrimitiveInt(100) > PrimitiveInt(42))
        assertFalse(PrimitiveDouble(42.42) >= PrimitiveDouble(100.0))

        assertTrue(XmlByte(42) <= XmlByte(100))
        assertFalse(XmlByte(42) > XmlByte(100))
        assertTrue(XmlByte(4242) > PrimitiveInt(100))

        assertTrue(XmlInt(42) <= XmlInteger(100))
        assertFalse(XmlInteger(42) > PrimitiveInt(100))

        assertTrue(XmlLong(42) <= XmlLong(100))
        assertFalse(XmlLong(42) > PrimitiveInt(100))

        assertTrue(XmlDouble(42.42) <= XmlDouble(100.1))
        assertTrue(XmlInt(42) < XmlDouble(100.1))
        assertFalse(XmlFloat(42.0) > XmlDouble(100.42))

        assertTrue(XmlDateTime(datetime) <= XmlDateTime(LocalDateTime.now()))
        assertFalse(XmlDateTime(datetime) > XmlDateTime(LocalDateTime.now()))

        assertTrue(XmlDate(LocalDate.of(2004, 12, 1)) <= XmlDate(LocalDate.of(2005, 1, 30)))
        assertFalse(XmlDateTime(LocalDateTime.of(2004, 12, 1, 12, 0 , 0)) > XmlDate(LocalDate.of(2005, 1, 30)))

        assertTrue(XmlTime(LocalTime.of(20, 12, 1)) <= XmlTime(LocalTime.of(20, 12, 30)))
        assertTrue(XmlTime(LocalTime.of(5, 1, 30)) > XmlDateTime(LocalDateTime.of(2004, 12, 1, 5, 0 , 50)))
    }
}