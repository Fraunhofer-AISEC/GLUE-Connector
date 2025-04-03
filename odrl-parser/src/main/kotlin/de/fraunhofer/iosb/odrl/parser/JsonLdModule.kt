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
            if (cacheCapacity <= 0)
                return SchemeRouter.defaultInstance()
            return SchemeRouter()
                // Warnings are due to a bug in titanium-json-ld: DefaultHttpLoader is protected
                .set("http", LRUDocumentCache(HttpLoader.defaultInstance(), cacheCapacity))
                .set("https", LRUDocumentCache(HttpLoader.defaultInstance(), cacheCapacity))
                .set("file", FileLoader())
        }
    }

}