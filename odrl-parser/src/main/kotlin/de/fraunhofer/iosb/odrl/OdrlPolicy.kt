package de.fraunhofer.iosb.odrl

import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import de.fraunhofer.iosb.odrl.glue.GlueIdentifiers
import de.fraunhofer.iosb.odrl.parser.JsonLd
import de.fraunhofer.iosb.odrl.parser.JsonLdBeanValidator
import io.konform.validation.Validation
import io.konform.validation.ValidationError
import io.konform.validation.jsonschema.minimum
import java.net.URI

@JsonLd(GlueIdentifiers.CONTEXT)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = OdrlPolicy::class)
@JsonTypeName(OdrlIdentifiers.POLICY)
@JsonSubTypes(
    Type(OdrlSet::class),
    Type(OdrlOffer::class),
    Type(OdrlAgreement::class),
)
open class OdrlPolicy(
    // Using the @field: annotation target allows Jackson to see these properties also in subclasses
    @field:JsonProperty("@id")
    val uid: URI = URI.create(""),

    @field:JsonProperty(OdrlIdentifiers.HAS_PERMISSION)
    @field:JsonSetter(nulls = Nulls.AS_EMPTY)
    val permission: List<OdrlPermission> = listOf(),

    @field:JsonProperty(OdrlIdentifiers.HAS_PROHIBITION)
    @field:JsonSetter(nulls = Nulls.AS_EMPTY)
    val prohibition: List<OdrlProhibition> = listOf(),

    @field:JsonProperty(OdrlIdentifiers.OBLIGATION)
    @field:JsonSetter(nulls = Nulls.AS_EMPTY)
    val obligation: List<OdrlDuty> = listOf(),

    @field:JsonProperty(OdrlIdentifiers.PROFILE)
    @field:JsonSetter(nulls = Nulls.AS_EMPTY)
    val profile: List<OdrlIri> = listOf(),

    @field:JsonProperty(OdrlIdentifiers.INHERIT_FROM)
    @field:JsonSetter(nulls = Nulls.AS_EMPTY)
    val inheritFrom: List<OdrlIri> = listOf(),

    @field:JsonProperty(OdrlIdentifiers.CONFLICT)
    val conflict: OdrlConflictTerm? = null,
) : JsonLdBeanValidator {

    override fun validate(): List<ValidationError> {
        return validateOdrlPolicy.validate(this).errors
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OdrlPolicy

        if (uid != other.uid) return false
        if (permission != other.permission) return false
        if (prohibition != other.prohibition) return false
        if (obligation != other.obligation) return false
        if (profile != other.profile) return false
        if (inheritFrom != other.inheritFrom) return false
        if (conflict != other.conflict) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uid.hashCode()
        result = 31 * result + permission.hashCode()
        result = 31 * result + prohibition.hashCode()
        result = 31 * result + obligation.hashCode()
        result = 31 * result + profile.hashCode()
        result = 31 * result + inheritFrom.hashCode()
        result = 31 * result + (conflict?.hashCode() ?: 0)
        return result
    }

    protected open fun getPropertyMap(): MutableMap<String, Any> {
        val map = mutableMapOf<String, Any>()
        map["uid"] = uid
        if (permission.isNotEmpty()) map["permission"] = permission
        if (prohibition.isNotEmpty()) map["prohibition"] = prohibition
        if (obligation.isNotEmpty()) map["obligation"] = obligation
        if (profile.isNotEmpty()) map["profile"] = profile
        if (inheritFrom.isNotEmpty()) map["inheritFrom"] = inheritFrom
        if (conflict != null) map["conflict"] = conflict
        return map
    }

    override fun toString(): String {
        return "OdrlPolicy(${getPropertyMap().entries.joinToString(", ")})"
    }
}

val validateOdrlPolicy = Validation<OdrlPolicy> {
    // A Policy MUST have one uid property value of type IRI
    OdrlPolicy::uid {
        addConstraint("OdrlPolicy: {value} is not a valid IRI") { isIri(it) }
    }

    // A Policy MUST have at least one permission, prohibition, or obligation property values
    OdrlPolicy::permission onEach {
        run(validateOdrlPermission)
    }
    OdrlPolicy::prohibition onEach {
        run(validateOdrlProhibition)
    }
    OdrlPolicy::obligation onEach {
        run(validateOdrlDuty)
    }
    validate("policyHasAtLeastOneRule", { it.permission.size + it.prohibition.size + it.obligation.size }) {
        minimum(1)
    }

    // A Policy MAY have none, one, or many profile property values of type IRI
    OdrlPolicy::profile onEach {
        run(validateOdrlIri)
    }

    // A Policy MAY have none, one, or many inheritFrom property values of type IRI
    OdrlPolicy::inheritFrom onEach {
        run(validateOdrlIri)
    }

    // A Policy MAY have none or one conflict property values of type conflict term
    OdrlPolicy::conflict ifPresent {
        run(validateOdrlConflictTerm)
    }
}