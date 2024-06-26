package de.fraunhofer.aisec.glue.models

import com.fasterxml.jackson.annotation.JsonProperty

data class Permission(
    @JsonProperty("http://www.w3.org/ns/odrl/2/action")
    val action: String,
    @JsonProperty("http://www.w3.org/ns/odrl/2/assignee")
    val assignee: String,
    @JsonProperty("http://www.w3.org/ns/odrl/2/constraint")
    val constraint: List<Constraint>,
    @JsonProperty("http://www.w3.org/ns/odrl/2/target")
    val target: String,
)
