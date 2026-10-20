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
import java.net.URI

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = GlueRestParameter::class)
@JsonTypeName(GlueIdentifiers.REST_PARAMETER)
class GlueRestParameter(
    // Properties of superclass GlueParameter
    value: GlueValue? = null,
    // Properties of GlueRestParameter
    @param:JsonProperty(GlueIdentifiers.HTTP_URL)
    val httpUrl: URI,
    @param:JsonProperty(GlueIdentifiers.HTTP_TYPE)
    val httpType: GlueHttpType = GlueHttpType.GET,
) : GlueParameter(value) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as GlueRestParameter

        if (httpUrl != other.httpUrl) return false
        if (httpType != other.httpType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + httpUrl.hashCode()
        result = 31 * result + httpType.hashCode()
        return result
    }

    override fun toString(): String = "GlueRestParameter(httpUrl=$httpUrl, httpType=$httpType, value=$value)"
}
