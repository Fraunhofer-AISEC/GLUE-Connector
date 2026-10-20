/*
 * Copyright 2024-2025 Fraunhofer AISEC and Fraunhofer IOSB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.fraunhofer.iosb.odrl

import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import de.fraunhofer.iosb.odrl.glue.GlueContainerPipeline
import de.fraunhofer.iosb.odrl.glue.GlueIdentifiers
import de.fraunhofer.iosb.odrl.glue.GlueValue
import de.fraunhofer.iosb.odrl.parser.JsonLd

@JsonLd(GlueIdentifiers.CONTEXT)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = OdrlRightOperand::class)
@JsonTypeName(OdrlIdentifiers.RIGHT_OPERAND)
@JsonSubTypes(
    // XML primitives
    Type(XmlBoolean::class),
    Type(XmlByte::class),
    Type(XmlInt::class),
    Type(XmlInteger::class),
    Type(XmlLong::class),
    Type(XmlDouble::class),
    Type(XmlFloat::class),
    Type(XmlString::class),
    Type(XmlDateTime::class),
    Type(XmlDate::class),
    Type(XmlTime::class),
    Type(XmlAnyUri::class),
    Type(XmlAnyType::class),
    // GLUE
    Type(GlueContainerPipeline::class),
    Type(GlueValue::class),
)
abstract class OdrlRightOperand : Comparable<OdrlRightOperand> {
    companion object {
        @JsonCreator @JvmStatic
        fun create(value: Boolean): OdrlRightOperand = PrimitiveBoolean(value)

        @JsonCreator @JvmStatic
        fun create(value: Int): OdrlRightOperand = PrimitiveInt(value)

        @JsonCreator @JvmStatic
        fun create(value: Double): OdrlRightOperand = PrimitiveDouble(value)

        @JsonCreator @JvmStatic
        fun create(value: String): OdrlRightOperand = PrimitiveString(value)
    }

    protected fun compareBoolean(
        value: Boolean,
        other: OdrlRightOperand,
    ): Int =
        when (other) {
            is PrimitiveBoolean -> value.compareTo(other.value)
            is XmlBoolean -> value.compareTo(other.value)
            else -> throw UnsupportedOperationException("Cannot compare Boolean to ${other.javaClass.simpleName}")
        }

    protected fun compareInt(
        value: Int,
        other: OdrlRightOperand,
    ): Int =
        when (other) {
            is PrimitiveInt -> value.compareTo(other.value)
            is PrimitiveDouble -> value.toDouble().compareTo(other.value)
            is XmlByte -> value.compareTo(other.value)
            is XmlInt -> value.compareTo(other.value)
            is XmlInteger -> value.compareTo(other.value)
            is XmlLong -> value.toLong().compareTo(other.value)
            is XmlDouble -> value.toDouble().compareTo(other.value)
            is XmlFloat -> value.toDouble().compareTo(other.value)
            else -> throw UnsupportedOperationException("Cannot compare Int to ${other.javaClass.simpleName}")
        }

    protected fun compareDouble(
        value: Double,
        other: OdrlRightOperand,
    ): Int =
        when (other) {
            is PrimitiveInt -> value.compareTo(other.value.toDouble())
            is PrimitiveDouble -> value.compareTo(other.value)
            is XmlByte -> value.compareTo(other.value.toDouble())
            is XmlInt -> value.compareTo(other.value.toDouble())
            is XmlInteger -> value.compareTo(other.value.toDouble())
            is XmlLong -> value.toLong().compareTo(other.value.toDouble())
            is XmlDouble -> value.compareTo(other.value)
            is XmlFloat -> value.compareTo(other.value)
            else -> throw UnsupportedOperationException("Cannot compare Double to ${other.javaClass.simpleName}")
        }

    protected fun compareString(
        value: String,
        other: OdrlRightOperand,
    ): Int =
        when (other) {
            is PrimitiveString -> value.compareTo(other.value)
            is XmlString -> value.compareTo(other.value)
            is XmlAnyUri -> value.compareTo(other.value.toString())
            is XmlAnyType -> value.compareTo(other.value.toString())
            else -> throw UnsupportedOperationException("Cannot compare String to ${other.javaClass.simpleName}")
        }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is OdrlRightOperand) return false
        return compareTo(other) == 0
    }

    override fun hashCode(): Int = javaClass.hashCode()
}

@JsonSerialize(using = PrimitiveSerializer::class)
data class PrimitiveBoolean(
    val value: Boolean,
) : OdrlRightOperand() {
    override fun compareTo(other: OdrlRightOperand) = compareBoolean(value, other)

    override fun equals(other: Any?) = super.equals(other)

    override fun hashCode() = value.hashCode()
}

@JsonSerialize(using = PrimitiveSerializer::class)
data class PrimitiveInt(
    val value: Int,
) : OdrlRightOperand() {
    override fun compareTo(other: OdrlRightOperand) = compareInt(value, other)

    override fun equals(other: Any?) = super.equals(other)

    override fun hashCode() = value.hashCode()
}

@JsonSerialize(using = PrimitiveSerializer::class)
data class PrimitiveDouble(
    val value: Double,
) : OdrlRightOperand() {
    override fun compareTo(other: OdrlRightOperand) = compareDouble(value, other)

    override fun equals(other: Any?) = super.equals(other)

    override fun hashCode() = value.hashCode()
}

@JsonSerialize(using = PrimitiveSerializer::class)
data class PrimitiveString(
    val value: String,
) : OdrlRightOperand() {
    override fun compareTo(other: OdrlRightOperand) = compareString(value, other)

    override fun equals(other: Any?) = super.equals(other)

    override fun hashCode() = value.hashCode()
}

class PrimitiveSerializer : StdSerializer<Any>(Any::class.java) {
    override fun serialize(
        value: Any,
        gen: JsonGenerator,
        serializers: SerializerProvider,
    ) {
        when (value) {
            is PrimitiveBoolean -> gen.writeBoolean(value.value)
            is PrimitiveInt -> gen.writeNumber(value.value)
            is PrimitiveDouble -> gen.writeNumber(value.value)
            is PrimitiveString -> gen.writeString(value.value)
        }
    }

    override fun serializeWithType(
        value: Any,
        gen: JsonGenerator,
        serializers: SerializerProvider,
        typeSer: TypeSerializer,
    ) {
        serialize(value, gen, serializers)
    }
}
