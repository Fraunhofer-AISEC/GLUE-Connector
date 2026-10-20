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

import ch.qos.logback.classic.LoggerContext
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory

/**
 * Registers shared logging dependencies in the Logback context.
 */
object LogbackObjectMapperInjector {
    const val CONTEXT_KEY = "glueObjectMapper"

    fun register(objectMapper: ObjectMapper) {
        val context = LoggerFactory.getILoggerFactory() as? LoggerContext ?: return
        context.putObject(CONTEXT_KEY, objectMapper)
    }
}
