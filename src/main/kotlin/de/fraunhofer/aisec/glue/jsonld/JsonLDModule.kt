package de.fraunhofer.aisec.glue.jsonld

import com.apicatalog.jsonld.JsonLdOptions
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsonp.JSONPModule
import com.fasterxml.jackson.module.kotlin.kotlinModule

/**
 * This module modifies an [ObjectMapper] to preprocess types annotated with [JsonLDType] as JsonLD before
 * deserializing.
 * Internally, the deserializers make use of an internal [ObjectMapper] accessible via the [mapper] property.
 * All required modules to serialize and deserialize JsonLD types need to be registered for this mapper.
 */
class JsonLDModule(private val options: JsonLdOptions, val mapper: ObjectMapper = DEFAULT_MAPPER) : SimpleModule() {
    override fun setupModule(context: SetupContext) {
        context.addBeanSerializerModifier(JsonLDBeanSerializerModifier(options, mapper))
        context.addBeanDeserializerModifier(JsonLDBeanDeserializerModifier(options, mapper))
    }

    companion object {
        val DEFAULT_MAPPER: JsonMapper =
            JsonMapper.builder()
                .addModules(kotlinModule(), JSONPModule())
                .build()
    }
}
