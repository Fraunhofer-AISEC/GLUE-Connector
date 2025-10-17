package de.fraunhofer.aisec.glue.models.negotiation

import com.fasterxml.jackson.annotation.JsonProperty

data class MultilanguageProperty(
    @param:JsonProperty("@language")
    val language: String,
    @param:JsonProperty("@value")
    val value: String,
)
