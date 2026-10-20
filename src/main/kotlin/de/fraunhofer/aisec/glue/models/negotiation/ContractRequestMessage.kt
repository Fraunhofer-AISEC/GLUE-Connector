/*
 * Copyright 2024-2026 Fraunhofer AISEC
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
package de.fraunhofer.aisec.glue.models.negotiation

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import de.fraunhofer.aisec.glue.jsonld.BoxedStringDeserializer
import de.fraunhofer.iosb.odrl.OdrlOffer
import de.fraunhofer.iosb.odrl.parser.JsonLd

@JsonLd(Contexts.DSPACE)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonTypeName("${Namespaces.DSPACE}/ContractRequestMessage")
data class ContractRequestMessage(
    @param:JsonProperty("@id")
    override val id: String,
    @param:JsonProperty("${Namespaces.DSPACE}/providerPid")
    val providerPid: String? = null,
    @param:JsonProperty("${Namespaces.DSPACE}/consumerPid")
    val consumerPid: String,
    @param:JsonProperty("${Namespaces.DSPACE}/callbackAddress")
    @param:JsonDeserialize(using = BoxedStringDeserializer::class)
    val callbackAddress: String,
    @param:JsonProperty("${Namespaces.DSPACE}/offer")
    val offer: OdrlOffer,
) : DspMessage
