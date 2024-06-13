package fhg.glue.odrl

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class OdrlLiteralParser: OdrlParser() {
    override val key: String = "literal"

    fun parse(root: JsonNode): OdrlLiteral<*> {
        if (root.isTextual)
            return OdrlLiteral<String>(root.asText())
        if (root.isInt)
            return OdrlLiteral<Int>(root.asInt())
        if (root.isBoolean)
            return OdrlLiteral<Boolean>(root.asBoolean())
        if (root.isDouble)
            return OdrlLiteral<Double>(root.asDouble())

        if (root.isArray) {
            val list = mutableListOf<Any?>()
            for (i in root.elements()) {
                if (i.isTextual)
                    list.add(i.asText())
                else if (i.isInt)
                    list.add(i.asInt())
                else if (i.isBoolean)
                    list.add(i.asBoolean())
                else if (i.isDouble)
                    list.add(i.asDouble())
                else
                    throw OdrlException("Invalid ODRL literal type in list")
            }
            return OdrlLiteral<List<*>>(list)
        }

        if (root.isObject) {
            val type = getStringOrThrow(root, "@type")
            val value = getStringOrThrow(root, "@value")

            if (type == "xsd:string" || type == "string")
                return OdrlLiteral<String>(value)

            if (type == "xsd:int" || type == "int" || type == "xsd:integer" || type == "integer")
                return OdrlLiteral<Int>(value.toInt())

            if (type == "xsd:bool" || type == "bool" || type == "xsd:boolean" || type == "boolean")
                return OdrlLiteral<Boolean>(value == "true" || value == "1");

            if (type == "xsd:double" || type == "double")
                return OdrlLiteral<Double>(value.toDouble())

            if (type == "xsd:datetime" || type == "datetime") {
                return OdrlLiteral<LocalDateTime>(LocalDateTime.parse(value))
            }

            if (type == "xsd:date" || type == "date") {
                return OdrlLiteral<LocalDateTime>(LocalDateTime.of(LocalDate.parse(value), LocalTime.of(0, 0)))
            }
        }

        throw OdrlException("Invalid ODRL literal")
    }
}