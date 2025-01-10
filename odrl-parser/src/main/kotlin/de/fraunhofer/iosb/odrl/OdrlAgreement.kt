package de.fraunhofer.iosb.odrl

import com.fasterxml.jackson.annotation.*
import de.fraunhofer.iosb.odrl.glue.GlueIdentifiers
import de.fraunhofer.iosb.odrl.parser.JsonLd
import de.fraunhofer.iosb.odrl.parser.JsonLdBeanValidator
import io.konform.validation.Validation
import io.konform.validation.ValidationError
import java.net.URI

@JsonLd(GlueIdentifiers.CONTEXT)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonTypeName(OdrlIdentifiers.AGREEMENT)
class OdrlAgreement(
    uid: URI = URI.create(""),
    permission: List<OdrlPermission> = listOf(),
    prohibition: List<OdrlProhibition> = listOf(),
    obligation: List<OdrlDuty> = listOf(),
    profile: List<OdrlIri> = listOf(),
    inheritFrom: List<OdrlIri> = listOf(),
    conflict: OdrlConflictTerm? = null,

    @JsonProperty(OdrlIdentifiers.ASSIGNER)
    val assigner: OdrlParty = OdrlParty(),

    @JsonProperty(OdrlIdentifiers.ASSIGNEE)
    val assignee: OdrlParty = OdrlParty(),
) : OdrlPolicy(uid, permission, prohibition, obligation, profile, inheritFrom, conflict),
    JsonLdBeanValidator {

    override fun validate(): List<ValidationError> {
        return validateOdrlAgreement.validate(this).errors
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as OdrlAgreement

        if (assigner != other.assigner) return false
        if (assignee != other.assignee) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + assigner.hashCode()
        result = 31 * result + assignee.hashCode()
        return result
    }

    override fun getPropertyMap(): MutableMap<String, Any> {
        val map = super.getPropertyMap()
        map["assigner"] = assigner
        map["assignee"] = assignee
        return map
    }

    override fun toString(): String {
        return "OdrlAgreement(${getPropertyMap().entries.joinToString(", ")})"
    }
}

val validateOdrlAgreement = Validation {
    run(validateOdrlPolicy)

    // An Agreement MUST have one assigner property value of type Party
    OdrlAgreement::assigner {
        run(validateOdrlParty)
    }

    // An Agreement MUST have one assignee property value of type Party
    OdrlAgreement::assignee {
        run(validateOdrlParty)
    }
}