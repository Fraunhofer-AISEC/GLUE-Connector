package de.fraunhofer.aisec.glue.models

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import de.fraunhofer.aisec.glue.jsonld.JsonLDType

@JsonLDType
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonTypeName("http://www.w3.org/ns/odrl/2/Policy")
data class Policy(
    @JsonProperty("http://www.w3.org/ns/odrl/2/uid")
    val uid: String,
    @JsonProperty("http://www.w3.org/ns/odrl/2/source")
    val source: String,
    @JsonProperty("http://www.w3.org/ns/odrl/2/permission")
    val permission: Permission,
)
