package de.fraunhofer.iosb.odrl

import com.fasterxml.jackson.annotation.*
import de.fraunhofer.iosb.odrl.glue.GlueIdentifiers
import de.fraunhofer.iosb.odrl.parser.JsonLd
import de.fraunhofer.iosb.odrl.parser.JsonLdBeanValidator
import io.konform.validation.Validation
import io.konform.validation.ValidationError

@JsonLd(GlueIdentifiers.CONTEXT)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = OdrlPartyCollection::class)
@JsonTypeName(OdrlIdentifiers.PARTY_COLLECTION)
data class OdrlPartyCollection(
    @param:JsonProperty(OdrlIdentifiers.SOURCE)
    val source: OdrlIri? = null,

    @param:JsonProperty(OdrlIdentifiers.REFINEMENT)
    @param:JsonSetter(nulls = Nulls.AS_EMPTY)
    val refinement: List<OdrlConstraintRepresentation> = listOf(),
): JsonLdBeanValidator {
    // Constructor necessary for Jackson to deserialize primitive type
    @Suppress("unused")
    constructor(primitiveValue: String) : this(OdrlIri(primitiveValue))

    override fun validate(): List<ValidationError> {
        return validateOdrlPartyCollection.validate(this).errors
    }

    private fun getPropertyMap(): MutableMap<String, Any> {
        val map = mutableMapOf<String, Any>()
        if (source != null) map["source"] = source
        if (refinement.isNotEmpty()) map["partOf"] = refinement
        return map
    }

    override fun toString(): String {
        return "OdrlPartyCollection(${getPropertyMap().entries.joinToString(", ")})"
    }
}

val validateOdrlPartyCollection = Validation {
    // A PartyCollection MAY have one source property value of type IRI
    OdrlPartyCollection::source ifPresent {
        run(validateOdrlIri)
    }

    // A PartyCollection MAY have none, one, or many refinement property values of type Constraint
    OdrlPartyCollection::refinement {
        addConstraint("OdrlPartyCollection: Refinement list must be of homogeneous types Constraint/LogicalConstraint") { list ->
            list.all { it is OdrlConstraint } || list.all { it is OdrlLogicalConstraint }
        }
    }
    OdrlPartyCollection::refinement onEach {
        ifInstanceOf<OdrlConstraint> {
            run(validateOdrlConstraint)
        }
        ifInstanceOf<OdrlLogicalConstraint> {
            run(validateOdrlLogicalConstraint)
        }
    }
}