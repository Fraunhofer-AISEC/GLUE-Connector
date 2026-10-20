/*
 * Copyright 2026 Fraunhofer AISEC
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
package de.fraunhofer.aisec.glue.logging

import ch.qos.logback.classic.pattern.ClassicConverter
import ch.qos.logback.classic.spi.ILoggingEvent
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.event.KeyValuePair

/**
 * Formats SLF4J key-value pairs on separate lines with optional pretty JSON rendering.
 */
class KeyValuePairConverter : ClassicConverter() {
    override fun convert(event: ILoggingEvent): String {
        val pairs = event.keyValuePairs ?: return ""
        if (pairs.isEmpty()) {
            return ""
        }

        val separator = System.lineSeparator()
        return pairs.joinToString(separator = separator, prefix = separator) { pair ->
            formatPair(pair)
        }
    }

    private fun formatPair(pair: KeyValuePair): String {
        val value = formatValue(pair.value)
        return "${pair.key}=$value"
    }

    private fun formatValue(value: Any?): String {
        if (value == null) {
            return "null"
        }

        if (hasJsonTypeInfo(value.javaClass)) {
            (context?.getObject(LogbackObjectMapperInjector.CONTEXT_KEY) as? ObjectMapper)?.let { objectMapper ->
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value)
            }
        }

        return value.toString()
    }

    private fun hasJsonTypeInfo(type: Class<*>): Boolean {
        val visited = HashSet<Class<*>>()
        val toVisit = ArrayDeque<Class<*>>()
        toVisit.add(type)

        while (toVisit.isNotEmpty()) {
            val current = toVisit.removeFirst()
            if (!visited.add(current)) {
                continue
            }
            if (current.isAnnotationPresent(JsonTypeInfo::class.java)) {
                return true
            }
            current.superclass?.let { toVisit.add(it) }
            current.interfaces.forEach { toVisit.add(it) }
        }

        return false
    }

    private fun looksCompactJson(raw: String): Boolean {
        if (raw.isEmpty()) {
            return false
        }
        if (raw.indexOf('\n') >= 0 || raw.indexOf('\r') >= 0) {
            return false
        }
        val first = raw.first()
        return first == '{' || first == '['
    }
}
