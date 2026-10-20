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

/**
 * Custom Logback converter that collapses the class name when it starts with the logger name.
 * Mimics the JUL formatter behavior: if sourceClassName starts with loggerName, only print the suffix.
 */
class CollapsedClassConverter : ClassicConverter() {
    override fun convert(event: ILoggingEvent): String {
        val className =
            try {
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
