/*
 * Copyright 2026 Fraunhofer AISEC and Fraunhofer IOSB
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

/**
 * A simple key-value parameter.
 *
 * Each instance carries a single `key=parameterValue` pair.
 * Multiple instances can be attached to a [GlueContainer] to pass
 * arbitrary configuration to the pipeline container.
 *
 * In the ODRL policy this appears as:
 * ```json
 * {
 *   "http://fhg.de/glue/parameter": [{
 *     "@type": ["http://fhg.de/glue/KeyValueParameter"],
 *     "http://fhg.de/glue/parameterKey": [{"@value": "listenEndpoint"}],
 *     "http://fhg.de/glue/parameterValue": [{"@value": "0.0.0.0:8080"}]
 *   }]
 * }
 * ```
 *
 * After JSON-LD compaction into `/config.json` the container sees:
 * ```json
 * {
 *   "type": "http://fhg.de/glue/KeyValueParameter",
 *   "http://fhg.de/glue/parameterKey": "listenEndpoint",
 *   "http://fhg.de/glue/parameterValue": "0.0.0.0:8080"
 * }
 * ```
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = GlueKeyValueParameter::class)
@JsonTypeName(GlueIdentifiers.KEY_VALUE_PARAMETER)
class GlueKeyValueParameter(
    @param:JsonProperty(GlueIdentifiers.PARAMETER_KEY)
    val key: String,
    @param:JsonProperty(GlueIdentifiers.PARAMETER_VALUE)
    val parameterValue: String,
    glueValue: GlueValue? = null,
) : GlueParameter(glueValue) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as GlueKeyValueParameter

        if (key != other.key) return false
        if (parameterValue != other.parameterValue) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + key.hashCode()
        result = 31 * result + parameterValue.hashCode()
        return result
    }

    override fun toString(): String = "GlueKeyValueParameter(key=$key, parameterValue=$parameterValue)"
}
