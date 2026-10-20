/*
 * Copyright 2025 Fraunhofer AISEC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
    @param:JsonProperty("${Namespaces.EDC}/filterExpression")
    val filterExpression: List<Any>,
    @param:JsonProperty("${Namespaces.EDC}/limit")
    val limit: Int? = null,
    @param:JsonProperty("${Namespaces.EDC}/offset")
    val offset: Int? = null,
    @param:JsonProperty("${Namespaces.EDC}/sortField")
    val sortField: String? = null,
    @param:JsonProperty("${Namespaces.EDC}/sortOrder")
    val sortOrder: SortOrder? = null,
) {
    enum class SortOrder {
        ASC,
        DESC,
    }
}
