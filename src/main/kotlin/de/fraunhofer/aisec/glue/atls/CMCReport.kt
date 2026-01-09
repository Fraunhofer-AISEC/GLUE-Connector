package de.fraunhofer.aisec.glue.atls

data class CMCReport(val report: String, val metadata: MetadataMap?, val cacheMisses: List<String>?)
