package de.fraunhofer.aisec.glue.models.edc

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import de.fraunhofer.iosb.odrl.parser.JsonLd

@JsonLd(Contexts.EDC)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonTypeName("${Namespaces.EDC}/QuerySpec")
data class QuerySpec(
    // This is currently unused, so the type is not modelled correctly
    @JsonProperty("${Namespaces.EDC}/filterExpression")
    val filterExpression: List<Any>,
    @JsonProperty("${Namespaces.EDC}/limit")
    val limit: Int? = null,
    @JsonProperty("${Namespaces.EDC}/offset")
    val offset: Int? = null,
    @JsonProperty("${Namespaces.EDC}/sortField")
    val sortField: String? = null,
    @JsonProperty("${Namespaces.EDC}/sortOrder")
    val sortOrder: SortOrder? = null,
) {
    enum class SortOrder {
        ASC,
        DESC,
    }
}
