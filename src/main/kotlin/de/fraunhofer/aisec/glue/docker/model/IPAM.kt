package de.fraunhofer.aisec.glue.docker.model

import com.fasterxml.jackson.annotation.JsonProperty

data class IPAM(
    @param:JsonProperty("Driver")
    val driver: String? = "default",
    @param:JsonProperty("Config")
    val config: List<IPAMConfig>? = null,
    @param:JsonProperty("Options")
    val options: Map<String, String>? = null,
)
