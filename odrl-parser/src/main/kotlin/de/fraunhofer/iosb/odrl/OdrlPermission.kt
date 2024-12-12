package de.fraunhofer.iosb.odrl

import com.fasterxml.jackson.annotation.*
import de.fraunhofer.iosb.odrl.glue.GlueIdentifiers
import de.fraunhofer.iosb.odrl.parser.JsonLd
import de.fraunhofer.iosb.odrl.parser.JsonLdBeanValidator
import io.konform.validation.Validation
import io.konform.validation.ValidationError

@JsonLd(GlueIdentifiers.CONTEXT)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = OdrlPermission::class)
@JsonTypeName(OdrlIdentifiers.PERMISSION)
class OdrlPermission: OdrlRule(), JsonLdBeanValidator {
    @JsonProperty(OdrlIdentifiers.HAS_DUTY)
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    val duty: List<OdrlDuty> = listOf()

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