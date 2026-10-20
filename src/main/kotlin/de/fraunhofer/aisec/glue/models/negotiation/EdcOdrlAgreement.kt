/*
 * Copyright 2026 Fraunhofer AISEC
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
import de.fraunhofer.iosb.odrl.*
import de.fraunhofer.iosb.odrl.glue.GlueIdentifiers
import de.fraunhofer.iosb.odrl.parser.JsonLd
import java.net.URI
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

@JsonLd(GlueIdentifiers.CONTEXT)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonTypeName(OdrlIdentifiers.AGREEMENT)
class EdcOdrlAgreement(
    // Properties of superclass OdrlPolicy
    uid: URI,
    permission: List<OdrlPermission> = listOf(),
    prohibition: List<OdrlProhibition> = listOf(),
    obligation: List<OdrlDuty> = listOf(),
    profile: List<OdrlIri> = listOf(),
    inheritFrom: List<OdrlIri> = listOf(),
    conflict: OdrlConflictTerm? = null,
    @field:JsonProperty("${Namespaces.DSPACE}/timestamp")
    val timestamp: OffsetDateTime =
        OffsetDateTime
            .now(ZoneOffset.UTC)
            .truncatedTo(ChronoUnit.SECONDS),
) : OdrlAgreement(uid, permission, prohibition, obligation, profile, inheritFrom, conflict) {
    override fun getPropertyMap(): MutableMap<String, Any> = super.getPropertyMap().apply { put("timestamp", timestamp) }

    override fun toString(): String = "EdcOdrlAgreement(${getPropertyMap().entries.joinToString(", ")})"

    companion object {
        fun from(policy: OdrlPolicy) =
            EdcOdrlAgreement(
                policy.uid,
                policy.permission,
                policy.prohibition,
                policy.obligation,
                policy.profile,
                policy.inheritFrom,
                policy.conflict,
            )
    }
}
