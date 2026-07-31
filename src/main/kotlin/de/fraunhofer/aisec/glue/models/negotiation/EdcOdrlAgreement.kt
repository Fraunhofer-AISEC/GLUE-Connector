package de.fraunhofer.aisec.glue.models.negotiation

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import de.fraunhofer.iosb.odrl.*
import java.net.URI
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

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
        OffsetDateTime.now(ZoneOffset.UTC)
            .truncatedTo(ChronoUnit.SECONDS),
) : OdrlAgreement(uid, permission, prohibition, obligation, profile, inheritFrom, conflict) {
    override fun getPropertyMap(): MutableMap<String, Any> {
        return super.getPropertyMap().apply { put("timestamp", timestamp) }
    }

    override fun toString(): String {
        return "EdcOdrlAgreement(${getPropertyMap().entries.joinToString(", ")})"
    }

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
