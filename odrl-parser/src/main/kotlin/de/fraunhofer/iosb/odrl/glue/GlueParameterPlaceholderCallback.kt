package de.fraunhofer.iosb.odrl.de.fraunhofer.iosb.odrl.glue

import jakarta.json.JsonValue

fun interface GlueParameterPlaceholderCallback {
    suspend fun getPlaceholderValue(key: String): JsonValue
}