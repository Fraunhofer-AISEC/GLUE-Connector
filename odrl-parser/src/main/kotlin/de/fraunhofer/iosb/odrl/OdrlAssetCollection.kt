package de.fraunhofer.iosb.odrl

import com.fasterxml.jackson.annotation.*
import de.fraunhofer.iosb.odrl.glue.GlueIdentifiers
import de.fraunhofer.iosb.odrl.parser.JsonLd
import de.fraunhofer.iosb.odrl.parser.JsonLdBeanValidator
import io.konform.validation.Validation
import io.konform.validation.ValidationError
import java.net.URI

@JsonLd(GlueIdentifiers.CONTEXT)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = OdrlAssetCollection::class)
@JsonTypeName(OdrlIdentifiers.ASSET_COLLECTION)
data class OdrlAssetCollection(
    @JsonProperty(OdrlIdentifiers.SOURCE)
    val source: URI? = null,

    @JsonProperty(OdrlIdentifiers.REFINEMENT)
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    val refinement: List<OdrlConstraintRepresentation> = listOf(),
): JsonLdBeanValidator {
    // Constructor necessary for Jackson to deserialize primitive type
    @Suppress("unused")
    constructor(primitiveValue: String) : this(URI.create(primitiveValue))

    override fun validate(): List<ValidationError> {
        return validateOdrlAssetCollection.validate(this).errors
    }

    private fun getPropertyMap(): MutableMap<String, Any> {
        val map = mutableMapOf<String, Any>()
        if (source != null) map["source"] = source
        if (refinement.isNotEmpty()) map["partOf"] = refinement
        return map
    }

    override fun toString(): String {
        return "OdrlAssetCollection(${getPropertyMap().entries.joinToString(", ")})"
    }
}

val validateOdrlAssetCollection = Validation {
    // An AssetCollection MAY have one source property value of type IRI
    OdrlAssetCollection::source ifPresent {
        addConstraint("OdrlAssetCollection: {value} is not a valid IRI") { isIri(it) }
    }

    // An AssetCollection MAY have none, one, or many refinement property values of type Constraint
    OdrlAssetCollection::refinement {
        addConstraint("OdrlAssetCollection: Refinement list must be of homogeneous types Constraint/LogicalConstraint") { list ->
            list.all { it is OdrlConstraint } || list.all { it is OdrlLogicalConstraint }
        }
    }
    OdrlAssetCollection::refinement onEach {
        ifInstanceOf<OdrlConstraint> {
            run(validateOdrlConstraint)
        }
        ifInstanceOf<OdrlLogicalConstraint> {
            run(validateOdrlLogicalConstraint)
        }
    }
}