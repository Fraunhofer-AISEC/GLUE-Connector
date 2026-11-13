/*
 * Copyright 2024-2025 Fraunhofer AISEC and Fraunhofer IOSB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
) : JsonLdBeanValidator {
    // Constructor necessary for Jackson to deserialize primitive type
    @Suppress("unused")
    constructor(primitiveValue: String) : this(URI.create(primitiveValue))

    override fun validate(): List<ValidationError> = validateOdrlAsset.validate(this).errors

    private fun getPropertyMap(): MutableMap<String, Any> {
        val map = mutableMapOf<String, Any>()
        if (uid != null) map["uid"] = uid
        if (partOf.isNotEmpty()) map["partOf"] = partOf
        return map
    }

    override fun toString(): String = "OdrlAsset(${getPropertyMap().entries.joinToString(", ")})"
}

val validateOdrlAsset =
    Validation {
        // An Asset SHOULD have one uid property value of type IRI
        OdrlAsset::uid ifPresent {
            constrain("OdrlAsset: {value} is not a valid IRI") { isIri(it) }
        }

        // An Asset MAY have none, one, or many partOf property values of type AssetCollection
        OdrlAsset::partOf onEach {
            run(validateOdrlAssetCollection)
        }
    }
