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
@JsonTypeName(OdrlIdentifiers.OFFER)
class OdrlOffer(
    // Properties of superclass OdrlPolicy
    @JsonProperty("@id")
    uid: URI = URI.create(""),

    @JsonProperty(OdrlIdentifiers.HAS_PERMISSION)
    permission: List<OdrlPermission> = listOf(),

    @JsonProperty(OdrlIdentifiers.HAS_PROHIBITION)
    prohibition: List<OdrlProhibition> = listOf(),

    @JsonProperty(OdrlIdentifiers.OBLIGATION)
    obligation: List<OdrlDuty> = listOf(),

    @JsonProperty(OdrlIdentifiers.PROFILE)
    profile: List<OdrlIri> = listOf(),

    @JsonProperty(OdrlIdentifiers.INHERIT_FROM)
    inheritFrom: List<OdrlIri> = listOf(),

    @JsonProperty(OdrlIdentifiers.CONFLICT)
    conflict: OdrlConflictTerm? = null,

    // Properties of OdrlAgreement
    @JsonProperty(OdrlIdentifiers.ASSIGNER)
    val assigner: OdrlParty = OdrlParty(),
) : OdrlPolicy(uid, permission, prohibition, obligation, profile, inheritFrom, conflict),
    JsonLdBeanValidator {

    override fun validate(): List<ValidationError> {
        return validateOdrlOffer.validate(this).errors
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as OdrlOffer

        return assigner == other.assigner
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + assigner.hashCode()
        return result
    }

    override fun getPropertyMap(): MutableMap<String, Any> {
        val map = super.getPropertyMap()
        map["assigner"] = assigner
        return map
    }

    override fun toString(): String {
        return "OdrlOffer(${getPropertyMap().entries.joinToString(", ")})"
    }
}

val validateOdrlOffer = Validation {
    run(validateOdrlPolicy)

    // An Offer MUST have one assigner property value of type Party
    OdrlOffer::assigner {
        run(validateOdrlParty)
    }
}