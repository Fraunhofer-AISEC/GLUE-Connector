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

@JsonLd(GlueIdentifiers.CONTEXT)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = OdrlAssetCollection::class)
@JsonTypeName(OdrlIdentifiers.ASSET_COLLECTION)
data class OdrlAssetCollection(
    @param:JsonProperty(OdrlIdentifiers.SOURCE)
    val source: OdrlIri? = null,
    @param:JsonProperty(OdrlIdentifiers.REFINEMENT)
    @param:JsonSetter(nulls = Nulls.AS_EMPTY)
    val refinement: List<OdrlConstraintRepresentation> = listOf(),
) : JsonLdBeanValidator {
    // Constructor necessary for Jackson to deserialize primitive type
    @Suppress("unused")
    constructor(primitiveValue: String) : this(OdrlIri(primitiveValue))

    override fun validate(): List<ValidationError> = validateOdrlAssetCollection.validate(this).errors

    private fun getPropertyMap(): MutableMap<String, Any> {
        val map = mutableMapOf<String, Any>()
        if (source != null) map["source"] = source
        if (refinement.isNotEmpty()) map["partOf"] = refinement
        return map
    }

    override fun toString(): String = "OdrlAssetCollection(${getPropertyMap().entries.joinToString(", ")})"
}

val validateOdrlAssetCollection =
    Validation {
        // An AssetCollection MAY have one source property value of type IRI
        OdrlAssetCollection::source ifPresent {
            run(validateOdrlIri)
        }

        // An AssetCollection MAY have none, one, or many refinement property values of type Constraint
        OdrlAssetCollection::refinement {
            constrain("OdrlAssetCollection: Refinement list must be of homogeneous types Constraint/LogicalConstraint") { list ->
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
