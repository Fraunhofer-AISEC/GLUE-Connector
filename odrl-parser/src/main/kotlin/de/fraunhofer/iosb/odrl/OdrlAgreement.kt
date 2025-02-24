package de.fraunhofer.iosb.odrl

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
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
    // Properties of superclass OdrlPolicy
    uid: URI = URI.create(""),
    permission: List<OdrlPermission> = listOf(),
    prohibition: List<OdrlProhibition> = listOf(),
    obligation: List<OdrlDuty> = listOf(),
    profile: List<OdrlIri> = listOf(),
    inheritFrom: List<OdrlIri> = listOf(),
    conflict: OdrlConflictTerm? = null,
) : OdrlPolicy(uid, permission, prohibition, obligation, profile, inheritFrom, conflict),
    JsonLdBeanValidator {

    override fun validate(): List<ValidationError> {
        return validateOdrlAgreement.validate(this).errors
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        return true
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun toString(): String {
        return "OdrlAgreement(${getPropertyMap().entries.joinToString(", ")})"
    }
}

val validateOdrlAgreement = Validation {
    run(validateOdrlPolicy)

    // An Agreement MUST have one assigner property value in each rule
    // An Agreement MUST have one assignee property value in each rule
    OdrlAgreement::permission onEach {
        OdrlPermission::assignee required {}
        OdrlPermission::assigner required {}
    }
    OdrlAgreement::prohibition onEach {
        OdrlProhibition::assignee required {}
        OdrlProhibition::assigner required {}
    }
    OdrlAgreement::obligation onEach {
        OdrlDuty::assignee required {}
        OdrlDuty::assigner required {}
    }
}