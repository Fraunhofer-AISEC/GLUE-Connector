package de.fraunhofer.aisec.glue.docker.model

import com.fasterxml.jackson.annotation.JsonProperty

data class ContainerSummaryNetworkSettings(
    @JsonProperty("Networks")
    val networks: Map<String, EndpointSettings>? = null
)
