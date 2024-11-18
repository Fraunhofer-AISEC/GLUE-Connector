package fhg.glue.odrl.de.fraunhofer.iosb.odrl

import com.fasterxml.jackson.annotation.*
import fhg.glue.odrl.de.fraunhofer.iosb.odrl.glue.GlueIdentifiers
import fhg.glue.odrl.de.fraunhofer.iosb.odrl.parser.JsonLd
import fhg.glue.odrl.de.fraunhofer.iosb.odrl.parser.JsonLdBeanValidator
import io.konform.validation.Validation
import io.konform.validation.ValidationError

@JsonLd(GlueIdentifiers.CONTEXT)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = OdrlDuty::class)
@JsonTypeName(OdrlIdentifiers.DUTY)
class OdrlDuty: OdrlRule(), JsonLdBeanValidator {
    @JsonProperty(OdrlIdentifiers.CONSEQUENCE)
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    val consequence: List<OdrlDuty> = listOf()

    override fun validate(): List<ValidationError> {
        return validateOdrlDuty.validate(this).errors
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as OdrlDuty

        return consequence == other.consequence
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + consequence.hashCode()
        return result
    }

    override fun getPropertyMap(): MutableMap<String, Any> {
        val map = super.getPropertyMap()
        if (consequence.isNotEmpty()) map["consequence"] = consequence
        return map
    }

    override fun toString(): String {
        return "OdrlDuty(${getPropertyMap().entries.joinToString(", ")})"
    }
}

val validateOdrlDuty = Validation {
    run(validateOdrlRule)

    // A Duty MAY have none or one target property values (of type Asset)
    // Other relation sub-properties MAY be used
    // Note: Already validated as part of Rule

    // A Duty MAY have none or one assigner and/or assignee property values of type Party
    // Other function sub-properties MAY be used
    // Note: Already validated as part of Rule

    // A Duty MAY have none, one or many consequence property values of type Duty
    OdrlDuty::consequence onEach {
        addConstraint("OdrlDuty: Invalid duty in property 'consequence'") { validateRecursively(it) }
    }
}

private fun validateRecursively(duty: OdrlDuty): Boolean {
    return validateOdrlDuty.validate(duty).isValid
}