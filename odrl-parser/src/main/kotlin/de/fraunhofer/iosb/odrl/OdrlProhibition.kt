package de.fraunhofer.iosb.odrl

import com.fasterxml.jackson.annotation.*
import de.fraunhofer.iosb.odrl.glue.GlueIdentifiers
import de.fraunhofer.iosb.odrl.parser.JsonLd
import de.fraunhofer.iosb.odrl.parser.JsonLdBeanValidator
import io.konform.validation.Validation
import io.konform.validation.ValidationError

@JsonLd(GlueIdentifiers.CONTEXT)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = OdrlPermission::class)
@JsonTypeName(OdrlIdentifiers.PROHIBITION)
class OdrlProhibition: OdrlRule(), JsonLdBeanValidator {
    @JsonProperty(OdrlIdentifiers.REMEDY)
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    val remedy: List<OdrlDuty> = listOf()

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