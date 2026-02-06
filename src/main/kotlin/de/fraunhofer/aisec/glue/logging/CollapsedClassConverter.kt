package de.fraunhofer.aisec.glue.logging

import ch.qos.logback.classic.pattern.ClassicConverter
import ch.qos.logback.classic.spi.ILoggingEvent

/**
 * Custom Logback converter that collapses the class name when it starts with the logger name.
 * Mimics the JUL formatter behavior: if sourceClassName starts with loggerName, only print the suffix.
 */
class CollapsedClassConverter : ClassicConverter() {
    override fun convert(event: ILoggingEvent): String {
        val className = try {
            event.callerData[0].className
        } catch (_: Throwable) {
            return "?"
        }
        val loggerName: String = event.loggerName
        return if (className.startsWith(loggerName)) {
            className.substring(loggerName.length)
        } else {
            className
        }
    }
}
