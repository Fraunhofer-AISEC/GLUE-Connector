package de.fraunhofer.iosb.odrl.de.fraunhofer.iosb.odrl.glue

import com.fasterxml.jackson.databind.JsonNode

fun interface GlueParameterPlaceholderCallback {
    suspend fun getPlaceholderValue(key: String): JsonNode
}