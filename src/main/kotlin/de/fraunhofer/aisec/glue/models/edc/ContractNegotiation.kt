/*
 * Copyright 2025-2026 Fraunhofer AISEC
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
import java.net.URI

@JsonLd(Contexts.EDC)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonTypeName("${Namespaces.EDC}/ContractNegotiation")
data class ContractNegotiation(
    @param:JsonProperty("@id")
    val uid: URI,
    @param:JsonProperty("${Namespaces.EDC}/assetId")
    val assetId: String,
    // The correct type is currently not mapped to any object
    @param:JsonProperty("${Namespaces.EDC}/callbackAddresses")
    val callbackAddresses: List<Any>,
    @param:JsonProperty("${Namespaces.EDC}/contractAgreementId")
    val contractAgreementId: URI? = null,
    @param:JsonProperty("${Namespaces.EDC}/counterPartyAddress")
    val counterPartyAddress: String,
    @param:JsonProperty("${Namespaces.EDC}/counterPartyId")
    val counterPartyId: URI,
    @param:JsonProperty("${Namespaces.EDC}/errorDetail")
    val errorDetail: String? = null,
    @param:JsonProperty("${Namespaces.EDC}/protocol")
    val protocol: String,
    @param:JsonProperty("${Namespaces.EDC}/state")
    val state: NegotiationState,
    @param:JsonProperty("${Namespaces.EDC}/type")
    val type: NegotiationType,
    @param:JsonProperty("${Namespaces.EDC}/createdAt")
    val createdAt: Long,
) {
    enum class NegotiationState {
        INITIAL,
        REQUESTING,
        REQUESTED,
        OFFERING,
        OFFERED,
        ACCEPTING,
        ACCEPTED,
        AGREEING,
        AGREED,
        VERIFYING,
        VERIFIED,
        FINALIZING,
        FINALIZED,
        TERMINATING,
        TERMINATED,
    }

    enum class NegotiationType {
        CONSUMER,
        PROVIDER,
    }
}
