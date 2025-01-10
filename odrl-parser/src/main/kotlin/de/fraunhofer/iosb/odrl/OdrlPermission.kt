package de.fraunhofer.iosb.odrl

import com.fasterxml.jackson.annotation.*
import de.fraunhofer.iosb.odrl.glue.GlueIdentifiers
import de.fraunhofer.iosb.odrl.parser.JsonLd
import de.fraunhofer.iosb.odrl.parser.JsonLdBeanValidator
import io.konform.validation.Validation
import io.konform.validation.ValidationError
import java.net.URI

@JsonLd(GlueIdentifiers.CONTEXT)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = OdrlPermission::class)
@JsonTypeName(OdrlIdentifiers.PERMISSION)
class OdrlPermission(
    // Properties of superclass OdrlRule
    @JsonProperty("@id")
    uid: URI? = null,

    @JsonProperty(OdrlIdentifiers.HAS_ACTION)
    action: OdrlAction = OdrlAction(URI.create("")),

    @JsonProperty(OdrlIdentifiers.TARGET)
    target: OdrlAsset? = null,

    @JsonProperty(OdrlIdentifiers.OUTPUT)
    output: OdrlAsset? = null,

    @JsonProperty(OdrlIdentifiers.ASSIGNEE)
    assignee: OdrlParty? = null,

    @JsonProperty(OdrlIdentifiers.ASSIGNER)
    assigner: OdrlParty? = null,

    @JsonProperty(OdrlIdentifiers.ATTRIBUTED_PARTY)
    attributedParty: OdrlParty? = null,

    @JsonProperty(OdrlIdentifiers.ATTRIBUTING_PARTY)
    attributingParty: OdrlParty? = null,

    @JsonProperty(OdrlIdentifiers.COMPENSATED_PARTY)
    compensatedParty: OdrlParty? = null,

    @JsonProperty(OdrlIdentifiers.COMPENSATING_PARTY)
    compensatingParty: OdrlParty? = null,

    @JsonProperty(OdrlIdentifiers.CONSENTED_PARTY)
    consentedParty: OdrlParty? = null,

    @JsonProperty(OdrlIdentifiers.CONSENTING_PARTY)
    consentingParty: OdrlParty? = null,

    @JsonProperty(OdrlIdentifiers.CONTRACTED_PARTY)
    contractedParty: OdrlParty? = null,

    @JsonProperty(OdrlIdentifiers.CONTRACTING_PARTY)
    contractingParty: OdrlParty? = null,

    @JsonProperty(OdrlIdentifiers.INFORMED_PARTY)
    informedParty: OdrlParty? = null,

    @JsonProperty(OdrlIdentifiers.INFORMING_PARTY)
    informingParty: OdrlParty? = null,

    @JsonProperty(OdrlIdentifiers.TRACKED_PARTY)
    trackedParty: OdrlParty? = null,

    @JsonProperty(OdrlIdentifiers.TRACKING_PARTY)
    trackingParty: OdrlParty? = null,

    @JsonProperty(OdrlIdentifiers.HAS_CONSTRAINT)
    constraint: List<OdrlConstraintRepresentation> = listOf(),

    // Properties of OdrlPermission
    @JsonProperty(OdrlIdentifiers.HAS_DUTY)
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    val duty: List<OdrlDuty> = listOf()
): OdrlRule(uid, action, target, output, assignee, assigner, attributedParty, attributingParty, compensatedParty,
        compensatingParty, consentedParty, consentingParty, contractedParty, contractingParty, informedParty,
        informingParty, trackedParty, trackingParty, constraint),
    JsonLdBeanValidator {

    override fun validate(): List<ValidationError> {
        return validateOdrlPermission.validate(this).errors
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as OdrlPermission

        return duty == other.duty
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + duty.hashCode()
        return result
    }

    override fun getPropertyMap(): MutableMap<String, Any> {
        val map = super.getPropertyMap()
        if (duty.isNotEmpty()) map["duty"] = duty
        return map
    }

    override fun toString(): String {
        return "OdrlPermission(${getPropertyMap().entries.joinToString(", ")})"
    }
}

val validateOdrlPermission = Validation<OdrlPermission> {
    run(validateOdrlRule)

    // A Permission MUST have one target property value of type Asset
    // Other relation sub-properties MAY be used
    addConstraint("OdrlPermission: target property is required") { it.target != null }

    // A Permission MAY have none or one assigner and/or assignee property values of type Party
    // Other function sub-properties MAY be used
    // Note: Already validated as part of Rule

    // A Permission MAY have none, one, or more duty property values of type Duty
    OdrlPermission::duty onEach {
        run(validateOdrlDuty)
    }
}