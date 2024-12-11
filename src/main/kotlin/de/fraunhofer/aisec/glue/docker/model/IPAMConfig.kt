package de.fraunhofer.aisec.glue.docker.model

import com.fasterxml.jackson.annotation.JsonProperty

data class IPAMConfig(
    @JsonProperty("Subnet")
    val subnet: String? = null,
    @JsonProperty("IPRange")
    val ipRange: String? = null,
    @JsonProperty("Gateway")
    val gateway: String? = null,
    @JsonProperty("AuxiliaryAddresses")
    val auxiliaryAddresses: Map<String, String>? = null,
)
