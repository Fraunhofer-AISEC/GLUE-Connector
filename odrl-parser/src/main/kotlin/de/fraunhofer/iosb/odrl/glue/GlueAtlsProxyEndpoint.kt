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
import de.fraunhofer.iosb.odrl.parser.JsonLd

@JsonLd(GlueIdentifiers.CONTEXT)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = GlueAtlsProxyEndpoint::class)
@JsonTypeName(GlueIdentifiers.ATLS_PROXY_ENDPOINT)
data class GlueAtlsProxyEndpoint(
    @param:JsonProperty(GlueIdentifiers.LISTEN_ENDPOINT)
    val listenEndpoint: String,
    @param:JsonProperty(GlueIdentifiers.NEXT_ENDPOINT)
    val nextEndpoint: String,
)
