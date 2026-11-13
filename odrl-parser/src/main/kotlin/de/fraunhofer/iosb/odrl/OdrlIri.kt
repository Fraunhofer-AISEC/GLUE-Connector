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

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import de.fraunhofer.iosb.odrl.glue.GlueIdentifiers
import de.fraunhofer.iosb.odrl.parser.JsonLd
import de.fraunhofer.iosb.odrl.parser.JsonLdBeanValidator
import io.konform.validation.Validation
import io.konform.validation.ValidationError
import java.net.URI

@JsonLd(GlueIdentifiers.CONTEXT)
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE, defaultImpl = OdrlIri::class)
data class OdrlIri(
    @param:JsonProperty("@id")
    val uid: URI,
) : JsonLdBeanValidator {
    // Constructor necessary for Jackson to deserialize primitive type
    @Suppress("unused")
    constructor(primitiveValue: String) : this(URI.create(primitiveValue))

    override fun validate(): List<ValidationError> = validateOdrlIri.validate(this).errors
}

fun isIri(uri: URI): Boolean = true

val validateOdrlIri =
    Validation {
        OdrlIri::uid {
            constrain("OdrlIri: {value} is not a valid IRI") { isIri(it) }
        }
    }
