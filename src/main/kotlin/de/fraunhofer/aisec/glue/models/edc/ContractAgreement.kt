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
import com.fasterxml.jackson.databind.JsonNode
import de.fraunhofer.iosb.odrl.parser.JsonLd

@JsonLd(Contexts.EDC)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonTypeName("${Namespaces.EDC}/ContractAgreement")
data class ContractAgreement(
    @param:JsonProperty("@id")
    val id: String,
    @param:JsonProperty("${Namespaces.EDC}/providerId")
    val providerId: String,
    @param:JsonProperty("${Namespaces.EDC}/consumerId")
    val consumerId: String,
    @param:JsonProperty("${Namespaces.EDC}/contractSigningDate")
    val contractSigningDate: Long,
    @param:JsonProperty("${Namespaces.EDC}/assetId")
    val assetId: String,
    @param:JsonProperty("${Namespaces.EDC}/policy")
    val policy: JsonNode,
)
