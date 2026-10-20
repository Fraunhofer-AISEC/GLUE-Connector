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

import com.apicatalog.jsonld.JsonLdOptions
import com.apicatalog.jsonld.loader.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule

class JsonLdModule(
    private val mapper: ObjectMapper = ObjectMapper(),
    private val options: JsonLdOptions = JsonLdOptions(),
    private val documentLoader: DocumentLoader = createDocumentLoader(),
) : SimpleModule() {
    override fun setupModule(context: SetupContext) {
        context.addBeanSerializerModifier(JsonLdBeanSerializerModifier(mapper, options, documentLoader))
        context.addBeanDeserializerModifier(JsonLdBeanDeserializerModifier(mapper, options, documentLoader))
        super.setupModule(context)
    }

    companion object {
        fun createDocumentLoader(cacheCapacity: Int = 10): DocumentLoader {
            if (cacheCapacity <= 0) {
                return SchemeRouter.defaultInstance()
            }
            return SchemeRouter()
                // Warnings are due to a bug in titanium-json-ld: DefaultHttpLoader is protected
                .set("http", LRUDocumentCache(HttpLoader.defaultInstance(), cacheCapacity))
                .set("https", LRUDocumentCache(HttpLoader.defaultInstance(), cacheCapacity))
                .set("file", FileLoader())
        }
    }
}
