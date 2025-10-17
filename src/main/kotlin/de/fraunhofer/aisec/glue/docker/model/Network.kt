package de.fraunhofer.aisec.glue.docker.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Network(
    @param:JsonProperty("Name")
    val name: String? = null,
    @param:JsonProperty("Id")
    val id: String? = null,
    @param:JsonProperty("Created")
    val created: String? = null,
    @param:JsonProperty("Scope")
    val scope: String? = null,
    @param:JsonProperty("Driver")
    val driver: String? = null,
    @param:JsonProperty("EnableIPv6")
    val enableIPv6: Boolean? = null,
    @param:JsonProperty("IPAM")
    val ipam: IPAM? = null,
    @param:JsonProperty("Internal")
    val `internal`: Boolean? = null,
    @param:JsonProperty("Attachable")
    val attachable: Boolean? = null,
    @param:JsonProperty("Ingress")
    val ingress: Boolean? = null,
    @param:JsonProperty("Containers")
    val containers: Map<String, NetworkContainer>? = null,
    @param:JsonProperty("Options")
    val options: Map<String, String>? = null,
    @param:JsonProperty("Labels")
    val labels: Map<String, String>? = null,
)
