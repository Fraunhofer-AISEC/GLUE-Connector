package de.fraunhofer.aisec.glue.jsonld

import com.apicatalog.jsonld.JsonLdOptions
import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.DeserializationConfig
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier

class JsonLDBeanDeserializerModifier(private val options: JsonLdOptions, private val mapper: ObjectMapper) : BeanDeserializerModifier() {
    override fun modifyDeserializer(
        config: DeserializationConfig,
        beanDesc: BeanDescription,
        deserializer: JsonDeserializer<*>,
    ): JsonDeserializer<*> {
        return if (beanDesc.beanClass.isAnnotationPresent(JsonLDType::class.java)) {
            JsonLDDeserializer(options, mapper, beanDesc.type, beanDesc.beanClass)
        } else {
            deserializer
        }
    }
}
