package de.fraunhofer.aisec.glue.pipeline.atls

import com.fasterxml.jackson.annotation.JsonProperty

data class AtlsProxyConnection(
    @param:JsonProperty("http://fhg.de/glue/localPort")
    val localPort: Int,
    @param:JsonProperty("http://fhg.de/glue/remoteAddress")
    val remoteAddress: String
)