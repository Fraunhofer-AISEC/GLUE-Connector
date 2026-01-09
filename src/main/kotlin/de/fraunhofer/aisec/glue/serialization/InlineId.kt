package de.fraunhofer.aisec.glue.serialization

import com.fasterxml.jackson.annotation.JsonUnwrapped

/**
 * Helper class to include an [id] field with an object.
 */
data class InlineId<K, V>(
    val id: K,
    @param:JsonUnwrapped
    @field:JsonUnwrapped
    val value: V,
)
