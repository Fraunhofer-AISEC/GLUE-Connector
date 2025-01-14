package de.fraunhofer.iosb.odrl

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = XmlBoolean::class)
@JsonTypeName(XmlIdentifiers.BOOLEAN)
data class XmlBoolean(
    @JsonProperty("@value")
    val value: Boolean,
): OdrlRightOperand() {
    override fun compareTo(other: OdrlRightOperand) = compareBoolean(value, other)
    override fun equals(other: Any?) = super.equals(other)
    override fun hashCode(): Int { return value.hashCode() }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = XmlByte::class)
@JsonTypeName(XmlIdentifiers.BYTE)
data class XmlByte(
    @JsonProperty("@value")
    val value: Int,
): OdrlRightOperand() {
    override fun compareTo(other: OdrlRightOperand) = compareInt(value, other)
    override fun equals(other: Any?) = super.equals(other)
    override fun hashCode(): Int { return value.hashCode() }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = XmlInt::class)
@JsonTypeName(XmlIdentifiers.INT)
data class XmlInt(
    @JsonProperty("@value")
    val value: Int,
): OdrlRightOperand() {
    override fun compareTo(other: OdrlRightOperand) = compareInt(value, other)
    override fun equals(other: Any?) = super.equals(other)
    override fun hashCode(): Int { return value.hashCode() }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = XmlInteger::class)
@JsonTypeName(XmlIdentifiers.INTEGER)
data class XmlInteger(
    @JsonProperty("@value")
    val value: Int,
): OdrlRightOperand() {
    override fun compareTo(other: OdrlRightOperand) = compareInt(value, other)
    override fun equals(other: Any?) = super.equals(other)
    override fun hashCode(): Int { return value.hashCode() }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = XmlLong::class)
@JsonTypeName(XmlIdentifiers.LONG)
data class XmlLong(
    @JsonProperty("@value")
    val value: Long,
): OdrlRightOperand() {
    override fun compareTo(other: OdrlRightOperand): Int {
        return when (other) {
            is PrimitiveInt -> value.compareTo(other.value.toLong())
            is PrimitiveDouble -> value.toDouble().compareTo(other.value)
            is XmlByte -> value.compareTo(other.value.toLong())
            is XmlInt -> value.compareTo(other.value.toLong())
            is XmlInteger -> value.compareTo(other.value.toLong())
            is XmlLong -> value.compareTo(other.value)
            is XmlDouble -> value.toDouble().compareTo(other.value)
            is XmlFloat -> value.toDouble().compareTo(other.value)
            else -> throw OdrlException("Cannot compare Long to ${other.javaClass.simpleName}")
        }
    }
    override fun equals(other: Any?) = super.equals(other)
    override fun hashCode(): Int { return value.hashCode() }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = XmlDouble::class)
@JsonTypeName(XmlIdentifiers.DOUBLE)
data class XmlDouble(
    @JsonProperty("@value")
    val value: Double,
): OdrlRightOperand() {
    override fun compareTo(other: OdrlRightOperand) = compareDouble(value, other)
    override fun equals(other: Any?) = super.equals(other)
    override fun hashCode(): Int { return value.hashCode() }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = XmlFloat::class)
@JsonTypeName(XmlIdentifiers.FLOAT)
data class XmlFloat(
    @JsonProperty("@value")
    val value: Double,
): OdrlRightOperand() {
    override fun compareTo(other: OdrlRightOperand) = compareDouble(value, other)
    override fun equals(other: Any?) = super.equals(other)
    override fun hashCode(): Int { return value.hashCode() }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = XmlString::class)
@JsonTypeName(XmlIdentifiers.STRING)
data class XmlString(
    @JsonProperty("@value")
    val value: String,
): OdrlRightOperand() {
    override fun compareTo(other: OdrlRightOperand) = compareString(value, other)
    override fun equals(other: Any?) = super.equals(other)
    override fun hashCode(): Int { return value.hashCode() }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = XmlDateTime::class)
@JsonTypeName(XmlIdentifiers.DATETIME)
data class XmlDateTime(
    @JsonProperty("@value")
    val value: LocalDateTime,
): OdrlRightOperand() {
    override fun compareTo(other: OdrlRightOperand): Int {
        return when (other) {
            is XmlDateTime -> value.compareTo(other.value)
            is XmlDate -> value.toLocalDate().compareTo(other.value)
            is XmlTime -> value.toLocalTime().compareTo(other.value)
            else -> throw OdrlException("Cannot compare DateTime to ${other.javaClass.simpleName}")
        }
    }
    override fun equals(other: Any?) = super.equals(other)
    override fun hashCode(): Int { return value.hashCode() }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = XmlDate::class)
@JsonTypeName(XmlIdentifiers.DATE)
data class XmlDate(
    @JsonProperty("@value")
    val value: LocalDate,
): OdrlRightOperand() {
    override fun compareTo(other: OdrlRightOperand): Int {
        return when (other) {
            is XmlDateTime -> value.compareTo(other.value.toLocalDate())
            is XmlDate -> value.compareTo(other.value)
            else -> throw OdrlException("Cannot compare Date to ${other.javaClass.simpleName}")
        }
    }
    override fun equals(other: Any?) = super.equals(other)
    override fun hashCode(): Int { return value.hashCode() }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = XmlTime::class)
@JsonTypeName(XmlIdentifiers.TIME)
data class XmlTime(
    @JsonProperty("@value")
    val value: LocalTime,
): OdrlRightOperand() {
    override fun compareTo(other: OdrlRightOperand): Int {
        return when (other) {
            is XmlDateTime -> value.compareTo(other.value.toLocalTime())
            is XmlTime -> value.compareTo(other.value)
            else -> throw OdrlException("Cannot compare Time to ${other.javaClass.simpleName}")
        }
    }
    override fun equals(other: Any?) = super.equals(other)
    override fun hashCode(): Int { return value.hashCode() }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = XmlAnyUri::class)
@JsonTypeName(XmlIdentifiers.ANYURI)
data class XmlAnyUri(
    @JsonProperty("@value")
    val value: URI,
): OdrlRightOperand() {
    // Constructor necessary for Jackson to deserialize primitive type
    @Suppress("unused")
    constructor(primitiveValue: String) : this(URI.create(primitiveValue))

    override fun compareTo(other: OdrlRightOperand) = compareString(value.toString(), other)
    override fun equals(other: Any?) = super.equals(other)
    override fun hashCode(): Int { return value.hashCode() }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = XmlAnyType::class)
@JsonTypeName(XmlIdentifiers.ANYTYPE)
data class XmlAnyType(
    @JsonProperty("@value")
    val value: URI,
): OdrlRightOperand() {
    // Constructor necessary for Jackson to deserialize primitive type
    @Suppress("unused")
    constructor(primitiveValue: String) : this(URI.create(primitiveValue))

    override fun compareTo(other: OdrlRightOperand) = compareString(value.toString(), other)
    override fun equals(other: Any?) = super.equals(other)
    override fun hashCode(): Int { return value.hashCode() }
}