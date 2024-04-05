package de.fraunhofer.aisec.glue.models

import kotlinx.serialization.Serializable

@Serializable
data class Pipeline(val id: String, val containers: List<Container>)
