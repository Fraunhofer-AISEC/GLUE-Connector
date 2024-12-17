package de.fraunhofer.aisec.glue.docker.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Network(
    @JsonProperty("Name")
    val name: String? = null,
    @JsonProperty("Id")
    val id: String? = null,
    @JsonProperty("Created")
    val created: String? = null,
    @JsonProperty("Scope")
    val scope: String? = null,
    @JsonProperty("Driver")
    val driver: String? = null,
    @JsonProperty("EnableIPv6")
    val enableIPv6: Boolean? = null,
    @JsonProperty("IPAM")
    val ipam: IPAM? = null,
    @JsonProperty("Internal")
    val `internal`: Boolean? = null,
    @JsonProperty("Attachable")
    val attachable: Boolean? = null,
    @JsonProperty("Ingress")
    val ingress: Boolean? = null,
    @JsonProperty("Containers")
    val containers: Map<String, NetworkContainer>? = null,
    @JsonProperty("Options")
    val options: Map<String, String>? = null,
    @JsonProperty("Labels")
    val labels: Map<String, String>? = null,
)
