package de.fraunhofer.aisec.glue.jsonld

import com.apicatalog.jsonld.JsonLdOptions
import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationConfig
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier

class JsonLDBeanSerializerModifier(private val options: JsonLdOptions, private val mapper: ObjectMapper) : BeanSerializerModifier() {
    override fun modifySerializer(
        config: SerializationConfig,
        beanDesc: BeanDescription,
        serializer: JsonSerializer<*>,
    ): JsonSerializer<*> {
        return if (beanDesc.beanClass.isAnnotationPresent(JsonLDType::class.java)) {
            JsonLDSerializer(options, mapper, beanDesc.beanClass)
        } else {
            serializer
        }
    }
}
