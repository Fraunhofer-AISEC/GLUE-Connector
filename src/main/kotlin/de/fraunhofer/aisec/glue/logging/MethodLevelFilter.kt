package de.fraunhofer.aisec.glue.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply

/**
 * Custom Logback filter that filters on the method level (class + method name pairs).
 * Mimics the JUL formatter behavior of filtering by (sourceClassName, sourceMethodName) pairs.
 */
class MethodLevelFilter : Filter<ILoggingEvent>() {
    private val filteredMethods = hashSetOf<Pair<String, String>>()

    fun setMethods(methods: String?) {
        if (methods.isNullOrBlank()) {
            return
        }
        filteredMethods.clear()
        addMethodsFrom(methods)
    }

    fun addMethod(method: String?) {
        if (method.isNullOrBlank()) {
            return
        }
        addMethodsFrom(method)
    }

    override fun decide(event: ILoggingEvent): FilterReply {
        if (event.callerData != null && event.callerData.size > 0) {
            val caller: StackTraceElement = event.callerData[0]
            if (filteredMethods.contains(Pair(caller.className, caller.methodName))) {
                return FilterReply.DENY
            }
        }
        return FilterReply.NEUTRAL
    }

    private fun addMethodsFrom(raw: String) {
        raw.lineSequence().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                return@forEach
            }
            val pair = parseEntry(trimmed)
            if (pair == null) {
                addWarn("Ignoring malformed method filter entry '$trimmed'. " +
                        "Use \"fully.qualified.ClassName.methodName\"!")
            } else {
                filteredMethods.add(pair)
            }
        }
    }

    private fun parseEntry(entry: String): Pair<String, String>? {
        val dotIndex = entry.lastIndexOf('.')
        if (dotIndex > 0 && dotIndex < entry.length - 1) {
            return Pair(entry.substring(0, dotIndex), entry.substring(dotIndex + 1))
        }
        return null
    }
}
