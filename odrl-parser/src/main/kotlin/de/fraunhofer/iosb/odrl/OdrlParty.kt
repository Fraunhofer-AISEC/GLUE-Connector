package fhg.glue.odrl.de.fraunhofer.iosb.odrl

import com.fasterxml.jackson.annotation.*
import fhg.glue.odrl.de.fraunhofer.iosb.odrl.glue.GlueIdentifiers
import fhg.glue.odrl.de.fraunhofer.iosb.odrl.parser.JsonLd
import fhg.glue.odrl.de.fraunhofer.iosb.odrl.parser.JsonLdBeanValidator
import io.konform.validation.Validation
import io.konform.validation.ValidationError
import java.net.URI

@JsonLd(GlueIdentifiers.CONTEXT)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = OdrlParty::class)
@JsonTypeName(OdrlIdentifiers.PARTY)
data class OdrlParty(
    @JsonProperty("@id")
    val uid: URI? = null,

    @JsonProperty(OdrlIdentifiers.PART_OF)
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    val partOf: List<OdrlPartyCollection> = listOf(),
): JsonLdBeanValidator {
    constructor(primitiveValue: String) : this(URI.create(primitiveValue))

    override fun validate(): List<ValidationError> {
        return validateOdrlParty.validate(this).errors
    }

    private fun getPropertyMap(): MutableMap<String, Any> {
        val map = mutableMapOf<String, Any>()
        if (uid != null) map["uid"] = uid
        if (partOf.isNotEmpty()) map["partOf"] = partOf
        return map
    }

    override fun toString(): String {
        return "OdrlParty(${getPropertyMap().entries.joinToString(", ")})"
    }
}

val validateOdrlParty = Validation {
    // A Party SHOULD have one uid property value of type IRI
    OdrlParty::uid ifPresent {
        addConstraint("OdrlParty: {value} is not a valid IRI") { isIri(it) }
    }

    // A Party MAY have none, one, or many partOf property values of type PartyCollection
    OdrlParty::partOf onEach {
        run(validateOdrlPartyCollection)
    }
}