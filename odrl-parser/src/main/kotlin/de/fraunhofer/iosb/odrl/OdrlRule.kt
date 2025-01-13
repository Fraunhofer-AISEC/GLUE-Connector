package de.fraunhofer.iosb.odrl

import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import de.fraunhofer.iosb.odrl.glue.GlueIdentifiers
import de.fraunhofer.iosb.odrl.parser.JsonLd
import de.fraunhofer.iosb.odrl.parser.JsonLdBeanValidator
import io.konform.validation.Validation
import io.konform.validation.ValidationError
import java.net.URI

@JsonLd(GlueIdentifiers.CONTEXT)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = OdrlRule::class)
@JsonTypeName(OdrlIdentifiers.RULE)
@JsonSubTypes(
    Type(OdrlPermission::class),
    Type(OdrlProhibition::class),
    Type(OdrlDuty::class),
)
open class OdrlRule(
    // Using the @field: annotation target allows Jackson to see these properties also in subclasses
    @field:JsonProperty("@id")
    val uid: URI? = null,

    @field:JsonProperty(OdrlIdentifiers.HAS_ACTION)
    val action: OdrlAction = OdrlAction(URI.create("")),

    @field:JsonProperty(OdrlIdentifiers.TARGET)
    val target: OdrlAsset? = null,

    @field:JsonProperty(OdrlIdentifiers.OUTPUT)
    val output: OdrlAsset? = null,

    @field:JsonProperty(OdrlIdentifiers.ASSIGNEE)
    val assignee: OdrlParty? = null,

    @field:JsonProperty(OdrlIdentifiers.ASSIGNER)
    val assigner: OdrlParty? = null,

    @field:JsonProperty(OdrlIdentifiers.ATTRIBUTED_PARTY)
    val attributedParty: OdrlParty? = null,

    @field:JsonProperty(OdrlIdentifiers.ATTRIBUTING_PARTY)
    val attributingParty: OdrlParty? = null,

    @field:JsonProperty(OdrlIdentifiers.COMPENSATED_PARTY)
    val compensatedParty: OdrlParty? = null,

    @field:JsonProperty(OdrlIdentifiers.COMPENSATING_PARTY)
    val compensatingParty: OdrlParty? = null,

    @field:JsonProperty(OdrlIdentifiers.CONSENTED_PARTY)
    val consentedParty: OdrlParty? = null,

    @field:JsonProperty(OdrlIdentifiers.CONSENTING_PARTY)
    val consentingParty: OdrlParty? = null,

    @field:JsonProperty(OdrlIdentifiers.CONTRACTED_PARTY)
    val contractedParty: OdrlParty? = null,

    @field:JsonProperty(OdrlIdentifiers.CONTRACTING_PARTY)
    val contractingParty: OdrlParty? = null,

    @field:JsonProperty(OdrlIdentifiers.INFORMED_PARTY)
    val informedParty: OdrlParty? = null,

    @field:JsonProperty(OdrlIdentifiers.INFORMING_PARTY)
    val informingParty: OdrlParty? = null,

    @field:JsonProperty(OdrlIdentifiers.TRACKED_PARTY)
    val trackedParty: OdrlParty? = null,

    @field:JsonProperty(OdrlIdentifiers.TRACKING_PARTY)
    val trackingParty: OdrlParty? = null,

    @field:JsonProperty(OdrlIdentifiers.HAS_CONSTRAINT)
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    val constraint: List<OdrlConstraintRepresentation> = listOf(),
): JsonLdBeanValidator {

    override fun validate(): List<ValidationError> {
        return validateOdrlRule.validate(this).errors
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OdrlRule

        if (uid != other.uid) return false
        if (action != other.action) return false
        if (target != other.target) return false
        if (output != other.output) return false
        if (assignee != other.assignee) return false
        if (assigner != other.assigner) return false
        if (attributedParty != other.attributedParty) return false
        if (attributingParty != other.attributingParty) return false
        if (compensatedParty != other.compensatedParty) return false
        if (compensatingParty != other.compensatingParty) return false
        if (consentedParty != other.consentedParty) return false
        if (consentingParty != other.consentingParty) return false
        if (contractedParty != other.contractedParty) return false
        if (contractingParty != other.contractingParty) return false
        if (informedParty != other.informedParty) return false
        if (informingParty != other.informingParty) return false
        if (trackedParty != other.trackedParty) return false
        if (trackingParty != other.trackingParty) return false
        if (constraint != other.constraint) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uid?.hashCode() ?: 0
        result = 31 * result + action.hashCode()
        result = 31 * result + (target?.hashCode() ?: 0)
        result = 31 * result + (output?.hashCode() ?: 0)
        result = 31 * result + (assignee?.hashCode() ?: 0)
        result = 31 * result + (assigner?.hashCode() ?: 0)
        result = 31 * result + (attributedParty?.hashCode() ?: 0)
        result = 31 * result + (attributingParty?.hashCode() ?: 0)
        result = 31 * result + (compensatedParty?.hashCode() ?: 0)
        result = 31 * result + (compensatingParty?.hashCode() ?: 0)
        result = 31 * result + (consentedParty?.hashCode() ?: 0)
        result = 31 * result + (consentingParty?.hashCode() ?: 0)
        result = 31 * result + (contractedParty?.hashCode() ?: 0)
        result = 31 * result + (contractingParty?.hashCode() ?: 0)
        result = 31 * result + (informedParty?.hashCode() ?: 0)
        result = 31 * result + (informingParty?.hashCode() ?: 0)
        result = 31 * result + (trackedParty?.hashCode() ?: 0)
        result = 31 * result + (trackingParty?.hashCode() ?: 0)
        result = 31 * result + constraint.hashCode()
        return result
    }

    protected open fun getPropertyMap(): MutableMap<String, Any> {
        val map = mutableMapOf<String, Any>()
        if (uid != null) map["uid"] = uid
        map["action"] = action
        if (target != null) map["target"] = target
        if (output != null) map["output"] = output
        if (assignee != null) map["assignee"] = assignee
        if (assigner != null) map["assigner"] = assigner
        if (attributedParty != null) map["attributedParty"] = attributedParty
        if (attributingParty != null) map["attributingParty"] = attributingParty
        if (compensatedParty != null) map["compensatedParty"] = compensatedParty
        if (compensatingParty != null) map["compensatingParty"] = compensatingParty
        if (consentedParty != null) map["consentedParty"] = consentedParty
        if (consentingParty != null) map["consentingParty"] = consentingParty
        if (contractedParty != null) map["contractedParty"] = contractedParty
        if (contractingParty != null) map["contractingParty"] = contractingParty
        if (informedParty != null) map["informedParty"] = informedParty
        if (informingParty != null) map["informingParty"] = informingParty
        if (trackedParty != null) map["trackedParty"] = trackedParty
        if (trackingParty != null) map["trackingParty"] = trackingParty
        if (constraint.isNotEmpty()) map["constraint"] = constraint
        return map
    }

    override fun toString(): String {
        return "OdrlRule(${getPropertyMap().entries.joinToString(", ")})"
    }
}

val validateOdrlRule = Validation {
    // A Rule MAY have none or one uid property values of type IRI
    OdrlRule::uid ifPresent {
        addConstraint("OdrlRule: {value} is not a valid IRI") { isIri(it) }
    }

    // A Rule MUST have one action property value of type Action
    OdrlRule::action {
        run(validateOdrlAction)
    }

    // A Rule MAY have none or one relation sub-property values of type Asset
    OdrlRule::output ifPresent {
        run(validateOdrlAsset)
    }
    OdrlRule::target ifPresent {
        run(validateOdrlAsset)
    }

    // A Rule MAY have none, one or many function sub-property values of type Party
    OdrlRule::assignee ifPresent {
        run(validateOdrlParty)
    }
    OdrlRule::assigner ifPresent {
        run(validateOdrlParty)
    }
    OdrlRule::attributedParty ifPresent {
        run(validateOdrlParty)
    }
    OdrlRule::attributingParty ifPresent {
        run(validateOdrlParty)
    }
    OdrlRule::compensatedParty ifPresent {
        run(validateOdrlParty)
    }
    OdrlRule::compensatingParty ifPresent {
        run(validateOdrlParty)
    }
    OdrlRule::consentedParty ifPresent {
        run(validateOdrlParty)
    }
    OdrlRule::consentingParty ifPresent {
        run(validateOdrlParty)
    }
    OdrlRule::contractedParty ifPresent {
        run(validateOdrlParty)
    }
    OdrlRule::contractingParty ifPresent {
        run(validateOdrlParty)
    }
    OdrlRule::informedParty ifPresent {
        run(validateOdrlParty)
    }
    OdrlRule::informingParty ifPresent {
        run(validateOdrlParty)
    }
    OdrlRule::trackedParty ifPresent {
        run(validateOdrlParty)
    }
    OdrlRule::trackingParty ifPresent {
        run(validateOdrlParty)
    }

    // A Rule MAY have none, one or many failure sub-property values of type Rule
    // Note: Failure sub-properties (consequence, remedy) are handled by subclasses (Duty, Prohibition)

    // A Rule MAY have none, one or many constraint property values of type Constraint/LogicalConstraint
    OdrlRule::constraint {
        addConstraint("OdrlRule: Constraint list must be of homogeneous types Constraint/LogicalConstraint") { list ->
            list.all { it is OdrlConstraint } || list.all { it is OdrlLogicalConstraint }
        }
    }
    OdrlRule::constraint onEach {
        ifInstanceOf<OdrlConstraint> {
            run(validateOdrlConstraint)
        }
        ifInstanceOf<OdrlLogicalConstraint> {
            run(validateOdrlLogicalConstraint)
        }
    }
}