package de.fraunhofer.aisec.glue.models

import com.fasterxml.jackson.annotation.JsonProperty

data class Constraint(
    @JsonProperty("http://www.w3.org/ns/odrl/2/leftOperand")
    val leftOperand: Any,
    @JsonProperty("http://www.w3.org/ns/odrl/2/operator")
    val operator: String,
    @JsonProperty("http://www.w3.org/ns/odrl/2/rightOperand")
    val rightOperand: Any
)
