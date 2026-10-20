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
package de.fraunhofer.iosb.odrl.glue

import com.fasterxml.jackson.annotation.*
import de.fraunhofer.iosb.odrl.parser.JsonLd
import de.fraunhofer.iosb.odrl.parser.JsonLdBeanValidator
import io.konform.validation.Validation
import io.konform.validation.ValidationError
import io.konform.validation.string.notBlank

@JsonLd(GlueIdentifiers.CONTEXT)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = GlueContainer::class)
@JsonTypeName(GlueIdentifiers.CONTAINER)
data class GlueContainer(
    @param:JsonProperty(GlueIdentifiers.IMAGE)
    val image: String,
    @param:JsonProperty(GlueIdentifiers.HASH)
    val hash: String,
    @param:JsonProperty(GlueIdentifiers.ALLOW_NETWORK)
    val allowNetwork: Boolean? = false,
    @param:JsonProperty(GlueIdentifiers.HAS_PARAMETER)
    @param:JsonSetter(nulls = Nulls.AS_EMPTY)
    val parameter: List<GlueParameter> = listOf(),
) : JsonLdBeanValidator {
    override fun validate(): List<ValidationError> = validateGlueContainer.validate(this).errors

    override fun toString(): String = "GlueContainer(image=$image, hash=${hash.substring(0, 8)}, parameter=$parameter)"
}

val validateGlueContainer =
    Validation {
        GlueContainer::image {
            notBlank()
        }

        GlueContainer::hash {
            notBlank()
        }
    }
