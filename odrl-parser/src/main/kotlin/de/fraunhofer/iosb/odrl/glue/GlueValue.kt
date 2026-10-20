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

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.databind.JsonNode
import de.fraunhofer.iosb.odrl.OdrlRightOperand

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = GlueValue::class)
@JsonTypeName(GlueIdentifiers.VALUE)
data class GlueValue(
    @param:JsonProperty(GlueIdentifiers.JSON)
    val json: JsonNode,
) : OdrlRightOperand() {
    override fun compareTo(other: OdrlRightOperand): Int =
        throw UnsupportedOperationException("Cannot compare GlueValue to ${other.javaClass.simpleName}")
}
