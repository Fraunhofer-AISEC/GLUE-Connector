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
package de.fraunhofer.iosb.odrl.parser

import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializationConfig
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier
import de.fraunhofer.iosb.odrl.OdrlAction
import de.fraunhofer.iosb.odrl.OdrlPolicy

class OdrlBeanSerializerModifier(
    private val compactSerialization: Boolean,
) : BeanSerializerModifier() {
    override fun modifySerializer(
        config: SerializationConfig?,
        beanDesc: BeanDescription?,
        serializer: JsonSerializer<*>,
    ): JsonSerializer<*> {
        val defaultSerializer = super.modifySerializer(config, beanDesc, serializer)
        if (beanDesc != null && OdrlAction::class.java.isAssignableFrom(beanDesc.beanClass)) {
            return OdrlActionSerializer()
        }
        if (compactSerialization) {
            if (beanDesc != null && OdrlPolicy::class.java.isAssignableFrom(beanDesc.beanClass)) {
                return OdrlPolicyCompactingSerializer(defaultSerializer, beanDesc.beanClass)
            }
        }
        return defaultSerializer
    }
}
