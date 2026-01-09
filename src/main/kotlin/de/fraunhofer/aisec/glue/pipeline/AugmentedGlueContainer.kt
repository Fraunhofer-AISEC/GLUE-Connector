package de.fraunhofer.aisec.glue.pipeline

import de.fraunhofer.iosb.odrl.glue.GlueContainer

data class AugmentedGlueContainer(
    val base: GlueContainer,
    val extraVolumes: Map<String, String>,
)
