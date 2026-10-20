/*
 * Copyright 2024-2026 Fraunhofer AISEC and Fraunhofer IOSB
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
import de.fraunhofer.iosb.odrl.OdrlRightOperand
import de.fraunhofer.iosb.odrl.parser.JsonLdBeanValidator
import io.konform.validation.Validation
import io.konform.validation.ValidationError
import io.konform.validation.jsonschema.minItems

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = GlueContainerPipeline::class)
@JsonTypeName(GlueIdentifiers.CONTAINER_PIPELINE)
data class GlueContainerPipeline(
    @param:JsonProperty(GlueIdentifiers.PLATFORM_SECURITY)
    @param:JsonSetter(nulls = Nulls.AS_EMPTY)
    val platformSecurity: List<String> = listOf(),
    @param:JsonProperty(GlueIdentifiers.HAS_CONTAINER)
    @param:JsonSetter(nulls = Nulls.AS_EMPTY)
    val container: List<GlueContainer> = listOf(),
    @param:JsonProperty(GlueIdentifiers.ATLS_PROXY_IN)
    val atlsProxyIn: GlueAtlsProxyConfiguration? = null,
    @param:JsonProperty(GlueIdentifiers.ATLS_PROXY_OUT)
    val atlsProxyOut: GlueAtlsProxyConfiguration? = null,
) : OdrlRightOperand(),
    JsonLdBeanValidator {
    override fun validate(): List<ValidationError> = validateGlueContainerPipeline.validate(this).errors

    override fun compareTo(other: OdrlRightOperand): Int =
        throw UnsupportedOperationException("Cannot compare values of type GlueContainerPipeline")

    override fun toString(): String = "GlueContainerPipeline(platform=$platformSecurity, container=[${container.joinToString(" -> ")}])"
}

val validateGlueContainerPipeline =
    Validation {
        GlueContainerPipeline::platformSecurity onEach {
            addConstraint("GlueContainerPipeline: {value} must be one of 'tpm2', 'sgx', 'tdx' or 'amd-sev'") {
                listOf("tpm2", "sgx", "tdx", "amd-sev").any { it2 -> it2.equals(it, false) }
            }
        }

        GlueContainerPipeline::container {
            minItems(1)
        }

        GlueContainerPipeline::container onEach {
            run(validateGlueContainer)
        }
    }
