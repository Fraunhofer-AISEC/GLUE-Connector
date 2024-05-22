package de.fraunhofer.aisec.glue.models

import kotlinx.serialization.Serializable

// TODO: Get this to represent the actual contract structure
@Serializable
data class Contract(val pipeline: Pipeline)
