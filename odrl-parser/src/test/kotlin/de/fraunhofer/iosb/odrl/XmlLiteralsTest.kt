package de.fraunhofer.iosb.odrl

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.assertThrows
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@Suppress("UnusedEquals", "RemoveExplicitTypeArguments")
class XmlLiteralsTest {
    @Test
    fun testBooleanEquality() {
        assertEquals(PrimitiveBoolean(false), PrimitiveBoolean(false))
        assertNotEquals(PrimitiveBoolean(true), PrimitiveBoolean(false))

        assertEquals(XmlBoolean(true), XmlBoolean(true))
        assertNotEquals(XmlBoolean(false), XmlBoolean(true))

        assertEquals<OdrlRightOperand>(XmlBoolean(true), PrimitiveBoolean(true))
        assertNotEquals<OdrlRightOperand>(XmlBoolean(false), PrimitiveBoolean(true))

        assertThrows<UnsupportedOperationException> { PrimitiveBoolean(false).equals(PrimitiveInt(0)) }
        assertThrows<UnsupportedOperationException> { XmlBoolean(true).equals(XmlInteger(1)) }
        assertThrows<UnsupportedOperationException> { XmlBoolean(true).equals(PrimitiveString("true")) }
    }

    @Test
    fun testIntegerEquality() {
        assertEquals(PrimitiveInt(42), PrimitiveInt(42))
        assertNotEquals(PrimitiveInt(42), PrimitiveInt(-1))

        assertEquals(XmlInt(Integer.MAX_VALUE), XmlInt(Integer.MAX_VALUE))
        assertNotEquals(XmlInt(Integer.MIN_VALUE), XmlInt(-1))

        assertEquals(XmlInteger(-1), XmlInteger(-1))
        assertNotEquals(XmlInteger(-1), XmlInteger(42))

        assertEquals(XmlByte(255), XmlByte(255))
        assertNotEquals(XmlByte(255), XmlByte(42))

        assertEquals(XmlLong(Long.MAX_VALUE), XmlLong(Long.MAX_VALUE))
        assertNotEquals(XmlLong(Long.MIN_VALUE), XmlLong(42))

        assertEquals<OdrlRightOperand>(PrimitiveInt(Integer.MAX_VALUE), XmlInt(Integer.MAX_VALUE))
        assertEquals<OdrlRightOperand>(XmlInt(-42), PrimitiveInt(-42))
        assertNotEquals<OdrlRightOperand>(XmlInt(43), PrimitiveInt(42))

        assertEquals<OdrlRightOperand>(PrimitiveInt(Integer.MAX_VALUE), XmlInteger(Integer.MAX_VALUE))
        assertEquals<OdrlRightOperand>(XmlInteger(-42), PrimitiveInt(-42))
        assertNotEquals<OdrlRightOperand>(XmlInteger(43), PrimitiveInt(42))

        assertEquals<OdrlRightOperand>(PrimitiveInt(255), XmlByte(255))
        assertEquals<OdrlRightOperand>(XmlByte(42), PrimitiveInt(42))
        assertNotEquals<OdrlRightOperand>(XmlByte(43), PrimitiveInt(42))

        assertEquals<OdrlRightOperand>(PrimitiveInt(Integer.MAX_VALUE), XmlLong(Integer.MAX_VALUE.toLong()))
        assertEquals<OdrlRightOperand>(XmlLong(-42), PrimitiveInt(-42))
        assertNotEquals<OdrlRightOperand>(XmlLong(43), PrimitiveInt(42))

        assertThrows<UnsupportedOperationException> { PrimitiveInt(0).equals(PrimitiveBoolean(false)) }
        assertThrows<UnsupportedOperationException> { XmlInteger(42).equals(PrimitiveString("42")) }
    }

    @Test
    fun testDoubleEquality() {
        assertEquals(PrimitiveDouble(42.42), PrimitiveDouble(42.42))
        assertNotEquals(PrimitiveDouble(0.42), PrimitiveDouble(42.42))

        assertEquals(XmlDouble(42.42), XmlDouble(42.42))
        assertNotEquals(XmlDouble(0.42), XmlDouble(42.42))

        assertEquals(XmlFloat(42.42F), XmlFloat(42.42F))
        assertNotEquals(XmlFloat(0.42F), XmlFloat(42.42F))

        assertEquals<OdrlRightOperand>(PrimitiveDouble(Double.MAX_VALUE), XmlDouble(Double.MAX_VALUE))
        assertEquals<OdrlRightOperand>(XmlDouble(-42.42), PrimitiveDouble(-42.42))
        assertNotEquals<OdrlRightOperand>(XmlDouble(43.0), PrimitiveDouble(42.42))

        assertEquals<OdrlRightOperand>(PrimitiveDouble(Float.MAX_VALUE.toDouble()), XmlFloat(Float.MAX_VALUE))
        assertEquals<OdrlRightOperand>(XmlFloat(-42.0F), PrimitiveDouble(-42.0))
        assertNotEquals<OdrlRightOperand>(XmlFloat(43.0F), PrimitiveDouble(42.42))

        assertEquals<OdrlRightOperand>(PrimitiveDouble(Integer.MAX_VALUE.toDouble()), XmlInteger(Integer.MAX_VALUE))
        assertEquals<OdrlRightOperand>(XmlDouble(-42.0), PrimitiveInt(-42))
        assertNotEquals<OdrlRightOperand>(XmlDouble(42.42), PrimitiveInt(42))

        assertThrows<UnsupportedOperationException> { XmlDouble(42.0).equals(PrimitiveString("42.0")) }
    }

    @Test
    fun testStringEquality() {
        assertEquals(PrimitiveString(""), PrimitiveString(""))
        assertEquals(PrimitiveString("test"), PrimitiveString("test"))
        assertNotEquals(PrimitiveString("test"), PrimitiveString(""))
        assertNotEquals(PrimitiveString(" "), PrimitiveString(""))

        assertEquals<OdrlRightOperand>(PrimitiveString("test"), XmlString("test"))
        assertEquals<OdrlRightOperand>(XmlString(" "), PrimitiveString(" "))
        assertNotEquals<OdrlRightOperand>(XmlString("teSt"), PrimitiveString("test"))

        assertThrows<UnsupportedOperationException> { PrimitiveString("true").equals(PrimitiveBoolean(true)) }
        assertThrows<UnsupportedOperationException> { PrimitiveString("0").equals(PrimitiveInt(0)) }
    }

    @Test
    fun testDatetimeEquality() {
        val datetime = LocalDateTime.now()
        assertEquals(XmlDateTime(datetime), XmlDateTime(datetime))
        assertNotEquals(XmlDateTime(LocalDateTime.now().minusDays(1)), XmlDateTime(datetime))

        val date = LocalDate.now()
        assertEquals(XmlDate(date), XmlDate(date))
        assertNotEquals(XmlDate(date.minusDays(1)), XmlDate(date))

        val time = LocalTime.now()
        assertEquals(XmlTime(time), XmlTime(time))
        assertNotEquals(XmlTime(time.minusHours(1)), XmlTime(time))

        assertEquals<OdrlRightOperand>(XmlDate(date), XmlDateTime(LocalDateTime.of(date, LocalTime.of(0, 42))))
        assertEquals<OdrlRightOperand>(XmlDateTime(LocalDateTime.of(date, LocalTime.of(0, 42))), XmlDate(date))

        assertEquals<OdrlRightOperand>(XmlTime(time), XmlDateTime(LocalDateTime.of(LocalDate.now(), time)))
        assertEquals<OdrlRightOperand>(XmlDateTime(LocalDateTime.of(LocalDate.now(), time)), XmlTime(time))

        assertThrows<UnsupportedOperationException> { XmlDate(date).equals(XmlTime(time)) }
        assertThrows<UnsupportedOperationException> { XmlTime(time).equals(XmlDate(date)) }
        assertThrows<UnsupportedOperationException> { XmlDateTime(datetime).equals(PrimitiveString(datetime.toString())) }
        assertThrows<UnsupportedOperationException> { XmlDateTime(datetime).equals(PrimitiveInt(0)) }
    }

    @Test
    fun testAnyUriEquality() {
        assertEquals(XmlAnyUri(URI.create("http://test.com")), XmlAnyUri(URI.create("http://test.com")))
        assertNotEquals(XmlAnyUri(URI.create("http://test.com")), XmlAnyUri(URI.create("http://example.com")))

        assertEquals<OdrlRightOperand>(XmlAnyUri(URI.create("http://test.com")), PrimitiveString("http://test.com"))
        assertNotEquals<OdrlRightOperand>(
            XmlAnyUri(URI.create("http://example.com")),
            PrimitiveString("http://test.com")
        )
    }

    @Test
    fun testAnyTypeEquality() {
        assertEquals(XmlAnyType(URI.create("http://test.com")), XmlAnyType(URI.create("http://test.com")))
        assertNotEquals(XmlAnyType(URI.create("http://test.com")), XmlAnyType(URI.create("http://example.com")))

        assertEquals<OdrlRightOperand>(XmlAnyType(URI.create("http://test.com")), PrimitiveString("http://test.com"))
        assertNotEquals<OdrlRightOperand>(
            XmlAnyType(URI.create("http://test.com")),
            PrimitiveString("http://example.com")
        )
    }

    @Test
    fun testBooleanComparison() {
        assertTrue(PrimitiveBoolean(true) > PrimitiveBoolean(false))
        assertFalse(PrimitiveBoolean(true) <= PrimitiveBoolean(false))

        assertTrue(XmlBoolean(true) > PrimitiveBoolean(false))
        assertFalse(XmlBoolean(true) <= PrimitiveBoolean(false))

        assertThrows<UnsupportedOperationException> { XmlBoolean(true) > PrimitiveInt(0) }
        assertThrows<UnsupportedOperationException> { XmlBoolean(true) > PrimitiveString("false") }
    }

    @Test
    fun testIntegerComparison() {
        assertTrue(PrimitiveInt(100) > PrimitiveInt(42))
        assertFalse(PrimitiveInt(0) <= PrimitiveInt(Integer.MIN_VALUE))

        assertTrue(XmlInt(Integer.MAX_VALUE) > XmlInt(42))
        assertFalse(XmlInt(0) <= XmlInt(-42))

        assertTrue(XmlInteger(100) < XmlInteger(4242))
        assertFalse(XmlInteger(-42) >= XmlInteger(-1))

        assertTrue(XmlByte(255) <= XmlByte(255))
        assertFalse(XmlByte(0) > XmlByte(1))

        assertTrue(XmlLong(255) <= XmlLong(4242))
        assertFalse(XmlLong(-42) >= XmlLong(-1))

        assertTrue(PrimitiveInt(4242) > XmlInteger(100))
        assertTrue(XmlInteger(-1) <= PrimitiveInt(0))
        assertFalse(XmlInteger(42) > PrimitiveInt(100))

        assertTrue(PrimitiveInt(4242) > XmlInt(100))
        assertTrue(XmlInt(-1) <= PrimitiveInt(Integer.MAX_VALUE))
        assertFalse(XmlInt(42) < PrimitiveInt(-1))

        assertTrue(PrimitiveInt(255) <= XmlByte(255))
        assertTrue(XmlByte(0) >= PrimitiveInt(-1))
        assertFalse(XmlByte(42) > PrimitiveInt(4242))

        assertTrue(PrimitiveInt(Integer.MAX_VALUE) <= XmlLong(Long.MAX_VALUE))
        assertTrue(XmlLong(-42) < PrimitiveInt(-1))
        assertFalse(XmlLong(Long.MIN_VALUE) > PrimitiveInt(Integer.MIN_VALUE))

        assertThrows<UnsupportedOperationException> { PrimitiveInt(1) >= PrimitiveBoolean(false) }
        assertThrows<UnsupportedOperationException> { XmlInteger(42) < PrimitiveString("100") }
    }

    @Test
    fun testDoubleComparison() {
        assertTrue(PrimitiveDouble(42.42) < PrimitiveDouble(4242.42))
        assertFalse(PrimitiveDouble(42.42) >= PrimitiveDouble(100.0))

        assertTrue(XmlDouble(Double.MIN_VALUE) > XmlDouble(0.0))
        assertFalse(XmlDouble(42.42) >= XmlDouble(Double.MAX_VALUE))

        assertTrue(XmlFloat(42.42F) >= XmlFloat(42.42F))
        assertFalse(XmlFloat(42.42F) <= XmlFloat(42.0F))

        assertTrue(PrimitiveDouble(42.42) <= XmlDouble(4242.42))
        assertTrue(XmlDouble(-42.42) < PrimitiveDouble(-0.42))
        assertFalse(XmlDouble(0.0) >= PrimitiveDouble(0.42))

        assertTrue(PrimitiveDouble(Double.MIN_VALUE) <= XmlFloat(0.42F))
        assertTrue(XmlFloat(0.42F) >= PrimitiveDouble(0.4))
        assertFalse(XmlFloat(Float.MIN_VALUE) < PrimitiveDouble(Double.MIN_VALUE))

        assertThrows<UnsupportedOperationException> { PrimitiveDouble(0.0) <= PrimitiveBoolean(true) }
        assertThrows<UnsupportedOperationException> { PrimitiveDouble(42.42) >= PrimitiveString("42.42") }
        assertThrows<UnsupportedOperationException> { XmlDouble(42.42) > PrimitiveString("42.0") }
        assertThrows<UnsupportedOperationException> { XmlFloat(0.0F) <= PrimitiveString("42") }
    }

    @Test
    fun testStringComparison() {
        assertTrue(PrimitiveString("abc") < PrimitiveString("def"))
        assertFalse(PrimitiveString("") >= PrimitiveString(" "))

        assertTrue(XmlString("abc") >= XmlString("abc"))
        assertFalse(XmlString("zzz") <= XmlString("yyy"))

        assertTrue(PrimitiveString("abc") > XmlString("ABC"))
        assertTrue(XmlString("abc") <= PrimitiveString("abc123"))
        assertFalse(XmlString("123") >= PrimitiveString("987"))

        assertThrows<UnsupportedOperationException> { PrimitiveString("true") >= PrimitiveBoolean(true) }
        assertThrows<UnsupportedOperationException> { PrimitiveString("0") <= PrimitiveInt(42) }
    }

    @Test
    fun testDatetimeComparison() {
        val datetime = LocalDateTime.now()
        assertTrue(XmlDateTime(datetime) <= XmlDateTime(datetime.plusMinutes(1)))
        assertFalse(XmlDateTime(datetime) > XmlDateTime(datetime))

        val date = LocalDate.now()
        assertTrue(XmlDate(date) <= XmlDate(date.plusDays(1)))
        assertFalse(XmlDate(date) > XmlDate(date))

        val time = LocalTime.now()
        assertTrue(XmlTime(time) <= XmlTime(time.plusMinutes(1)))
        assertFalse(XmlTime(time) > XmlTime(time))

        assertTrue(XmlDateTime(datetime) > XmlTime(datetime.toLocalTime().minusMinutes(1)))
        assertFalse(XmlTime(datetime.toLocalTime().plusMinutes(1)) <= XmlDateTime(datetime))

        assertTrue(XmlDateTime(datetime) < XmlDate(datetime.toLocalDate().plusDays(1)))
        assertFalse(XmlDate(datetime.toLocalDate().minusDays(1)) >= XmlDateTime(datetime))

        assertThrows<UnsupportedOperationException> { XmlDate(date) >= XmlTime(time) }
        assertThrows<UnsupportedOperationException> { XmlTime(time) <= XmlDate(date) }
        assertThrows<UnsupportedOperationException> { XmlDateTime(datetime) <= PrimitiveString(datetime.toString()) }
        assertThrows<UnsupportedOperationException> { XmlDateTime(datetime) >= PrimitiveInt(0) }
    }
}