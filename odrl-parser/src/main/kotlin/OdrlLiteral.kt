package fhg.glue.odrl

import java.time.LocalDateTime

class OdrlLiteral<T>(val value: T): Comparable<OdrlLiteral<*>> {
    init {
        if (value !is Int && value !is String && value !is Boolean && value !is Double && value !is LocalDateTime && value !is List<*>)
            throw OdrlException("Invalid ODRL literal type")
        if (value is List<*> && !value.all { it is Int || it is String || it is Boolean || it is Double || it is LocalDateTime })
            throw OdrlException("Invalid ODRL literal type in list")
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is OdrlLiteral<*>) return false
        return compareTo(other) == 0
    }

    override fun compareTo(other: OdrlLiteral<*>): Int {
        when (other.value) {
            is Int -> return compareToInt(other.value)
            is String -> return compareToString(other.value)
            is Boolean -> return compareToBoolean(other.value)
            is Double -> return compareToDouble(other.value)
            is LocalDateTime -> return compareToDateTime(other.value)
            is List<*> -> return compareToList(other.value)
        }
        throw OdrlException("Invalid type for ODRL literal")
    }

    private fun compareToInt(other: Int): Int {
        when (value) {
            is Int -> return value.compareTo(other)
            is String -> return value.toInt().compareTo(other)
            is Boolean -> return if (value == (other != 0)) 0 else -1
            is Double -> return value.toInt().compareTo(other)
            is LocalDateTime -> throw OdrlException("Cannot compare DateTime to Integer")
            is List<*> -> throw OdrlException("Cannot compare List to Integer")
        }
        throw OdrlException("Invalid type for ODRL literal")
    }

    private fun compareToString(other: String): Int {
        when (value) {
            is String -> return value.compareTo(other)
            is Int -> return value.compareTo(other.toInt())
            is Boolean -> return if (value == (other == "true")) 0 else -1
            is Double -> return value.compareTo(other.toDouble())
            is LocalDateTime -> throw OdrlException("Cannot compare DateTime to String")
            is List<*> -> throw OdrlException("Cannot compare List to String")
        }
        throw OdrlException("Invalid type for ODRL literal")
    }

    private fun compareToBoolean(other: Boolean): Int {
        when (value) {
            is Int -> return if ((value != 0) == other) 0 else 1
            is String -> return if ((value == "true") == other) 0 else 1
            is Boolean -> return value.compareTo(other)
            is Double -> return if ((value != 0.0) == other) 0 else 1
            is LocalDateTime -> throw OdrlException("Cannot compare DateTime to Boolean")
            is List<*> -> throw OdrlException("Cannot compare List to Boolean")
        }
        throw OdrlException("Invalid type for ODRL literal")
    }

    private fun compareToDouble(other: Double): Int {
        when (value) {
            is Int -> return value.compareTo(other)
            is String -> return value.toDouble().compareTo(other)
            is Boolean -> return if (value == (other != 0.0)) 0 else -1
            is Double -> return value.compareTo(other)
            is LocalDateTime -> throw OdrlException("Cannot compare DateTime to Double")
            is List<*> -> throw OdrlException("Cannot compare List to Double")
        }
        throw OdrlException("Invalid type for ODRL literal")
    }

    private fun compareToDateTime(other: LocalDateTime): Int {
        when (value) {
            is String -> throw OdrlException("Cannot compare String to DateTime")
            is Int -> throw OdrlException("Cannot compare Integer to DateTime")
            is Boolean -> throw OdrlException("Cannot compare Boolean to DateTime")
            is Double -> throw OdrlException("Cannot compare Double to DateTime")
            is LocalDateTime -> return value.compareTo(other)
            is List<*> -> throw OdrlException("Cannot compare List to DateTime")
        }
        throw OdrlException("Invalid type for ODRL literal")
    }

    private fun compareToList(other: List<*>): Int {
        when (value) {
            is String -> throw OdrlException("Cannot compare String to List")
            is Int -> throw OdrlException("Cannot compare Integer to List")
            is Boolean -> throw OdrlException("Cannot compare Boolean to List")
            is Double -> throw OdrlException("Cannot compare Double to List")
            is LocalDateTime -> throw OdrlException("Cannot compare DateTime to List")
            is List<*> -> return if (value == other) 0 else 1
        }
        throw OdrlException("Invalid type for ODRL literal")
    }

    fun hasPart(other: OdrlLiteral<*>): Boolean {
        if (value !is List<*>)
            throw OdrlException("Can evaluate hasPart operator only on list and literal")
        return value.contains(other.value)
    }

    fun isPartOf(other: OdrlLiteral<*>): Boolean {
        if (other.value !is List<*>)
            throw OdrlException("Can evaluate isPartOf operator only on literal and list")
        return other.value.contains(value)
    }

    override fun hashCode(): Int {
        return value?.hashCode() ?: 0
    }
}