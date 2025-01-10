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
@JsonTypeName(OdrlIdentifiers.PROHIBITION)
class OdrlProhibition(
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

    // Properties of OdrlProhibition
    @JsonProperty(OdrlIdentifiers.REMEDY)
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    val remedy: List<OdrlDuty> = listOf()
): OdrlRule(uid, action, target, output, assignee, assigner, attributedParty, attributingParty, compensatedParty,
        compensatingParty, consentedParty, consentingParty, contractedParty, contractingParty, informedParty,
        informingParty, trackedParty, trackingParty, constraint),
    JsonLdBeanValidator {

    override fun validate(): List<ValidationError> {
        return validateOdrlProhibition.validate(this).errors
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as OdrlProhibition

        return remedy == other.remedy
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + remedy.hashCode()
        return result
    }

    override fun getPropertyMap(): MutableMap<String, Any> {
        val map = super.getPropertyMap()
        if (remedy.isNotEmpty()) map["remedy"] = remedy
        return map
    }

    override fun toString(): String {
        return "OdrlProhibition(${getPropertyMap().entries.joinToString(", ")})"
    }
}

val validateOdrlProhibition = Validation<OdrlProhibition> {
    run(validateOdrlRule)

    // A Permission MUST have one target property value of type Asset
    // Other relation sub-properties MAY be used
    addConstraint("OdrlProhibition: target property is required") { it.target != null }

    // A Prohibition MAY have none or one assigner and/or assignee property values of type Party
    // Other function sub-properties MAY be used
    // Note: Already validated as part of Rule

    // A Prohibition MAY have none, one, or more remedy property values of type Duty
    OdrlProhibition::remedy onEach {
        run(validateOdrlDuty)
    }
}