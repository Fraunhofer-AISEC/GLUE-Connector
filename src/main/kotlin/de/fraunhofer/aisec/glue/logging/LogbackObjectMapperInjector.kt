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
