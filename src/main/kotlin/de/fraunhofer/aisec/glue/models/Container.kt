package de.fraunhofer.aisec.glue.models

import kotlinx.serialization.Serializable

@Serializable
data class Container(val name: String, val image: String, val cmd: List<String>)
