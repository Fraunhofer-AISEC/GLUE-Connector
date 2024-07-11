package de.fraunhofer.aisec.glue.models.negotiation

import com.fasterxml.jackson.annotation.JsonProperty

data class MultilanguageProperty(
    @JsonProperty("@language")
    val language: String,
    @JsonProperty("@value")
    val value: String,
)
