package de.fraunhofer.aisec.glue.docker.model

import com.fasterxml.jackson.annotation.JsonProperty

data class IPAM(
    @JsonProperty("Driver")
    val driver: String? = "default",
    @JsonProperty("Config")
    val config: List<IPAMConfig>? = null,
    @JsonProperty("Options")
    val options: Map<String, String>? = null,
)
