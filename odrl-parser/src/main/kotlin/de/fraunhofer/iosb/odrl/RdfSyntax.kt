package de.fraunhofer.iosb.odrl

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import java.net.URI

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = RdfValue::class)
@JsonTypeName(RdfIdentifiers.VALUE)
data class RdfValue(
    @JsonProperty("@id")
    val uid: URI,
) {
    // Constructor necessary for Jackson to deserialize primitive type
    @Suppress("unused")
    constructor(primitiveValue: String) : this(URI.create(primitiveValue))
}