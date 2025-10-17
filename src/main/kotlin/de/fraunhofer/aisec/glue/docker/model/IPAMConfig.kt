package de.fraunhofer.aisec.glue.docker.model

import com.fasterxml.jackson.annotation.JsonProperty

data class IPAMConfig(
    @param:JsonProperty("Subnet")
    val subnet: String? = null,
    @param:JsonProperty("IPRange")
    val ipRange: String? = null,
    @param:JsonProperty("Gateway")
    val gateway: String? = null,
    @param:JsonProperty("AuxiliaryAddresses")
    val auxiliaryAddresses: Map<String, String>? = null,
)
