package de.fraunhofer.aisec.glue.models

import com.fasterxml.jackson.annotation.JsonProperty
import de.fraunhofer.aisec.glue.jsonld.JsonLdId

@JsonLdId("https://www.aisec.fraunhofer.com/odrl-distribute-to-printer")
data class Policy(
    @JsonProperty("http://www.w3.org/ns/odrl/2/uid")
    val uid: String,
    @JsonProperty("http://www.w3.org/ns/odrl/2/source")
    val source: String,
    @JsonProperty("http://www.w3.org/ns/odrl/2/permission")
    val permission: Permission,
)
