package de.fraunhofer.iosb.odrl

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.net.URI

@JsonTypeInfo(use = JsonTypeInfo.Id.NONE, defaultImpl = RdfValue::class)
data class RdfValue(
    @JsonProperty("@id")
    val uid: URI,
) {
    // Constructor necessary for Jackson to deserialize primitive type
    @Suppress("unused")
    constructor(primitiveValue: String) : this(URI.create(primitiveValue))
}