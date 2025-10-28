package de.fraunhofer.iosb.odrl

import com.fasterxml.jackson.annotation.*
import de.fraunhofer.iosb.odrl.glue.GlueIdentifiers
import de.fraunhofer.iosb.odrl.parser.JsonLd
import de.fraunhofer.iosb.odrl.parser.JsonLdBeanValidator
import io.konform.validation.Validation
import io.konform.validation.ValidationError
import java.net.URI

@JsonLd(GlueIdentifiers.CONTEXT)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = OdrlAsset::class)
@JsonTypeName(OdrlIdentifiers.ASSET)
data class OdrlAsset(
    @param:JsonProperty("@id")
    val uid: URI? = null,

    @param:JsonProperty(OdrlIdentifiers.PART_OF)
    @param:JsonSetter(nulls = Nulls.AS_EMPTY)
    val partOf: List<OdrlAssetCollection> = listOf(),
): JsonLdBeanValidator {
    // Constructor necessary for Jackson to deserialize primitive type
    @Suppress("unused")
    constructor(primitiveValue: String) : this(URI.create(primitiveValue))

    override fun validate(): List<ValidationError> {
        return validateOdrlAsset.validate(this).errors
    }

    private fun getPropertyMap(): MutableMap<String, Any> {
        val map = mutableMapOf<String, Any>()
        if (uid != null) map["uid"] = uid
        if (partOf.isNotEmpty()) map["partOf"] = partOf
        return map
    }

    override fun toString(): String {
        return "OdrlAsset(${getPropertyMap().entries.joinToString(", ")})"
    }
}

val validateOdrlAsset = Validation {
    // An Asset SHOULD have one uid property value of type IRI
    OdrlAsset::uid ifPresent {
        addConstraint("OdrlAsset: {value} is not a valid IRI") { isIri(it) }
    }

    // An Asset MAY have none, one, or many partOf property values of type AssetCollection
    OdrlAsset::partOf onEach {
        run(validateOdrlAssetCollection)
    }
}