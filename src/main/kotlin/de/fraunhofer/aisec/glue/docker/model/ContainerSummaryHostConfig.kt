package de.fraunhofer.aisec.glue.docker.model

import com.fasterxml.jackson.annotation.JsonProperty

data class ContainerSummaryHostConfig(
    @JsonProperty("NetworkMode")
    val networkMode: String? = null,
)
