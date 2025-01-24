package de.fraunhofer.iosb.odrl.parser

import com.apicatalog.jsonld.JsonLdOptions
import com.apicatalog.jsonld.loader.DocumentLoader
import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationConfig
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier

class JsonLdBeanSerializerModifier(
    private val mapper: ObjectMapper,
    private val options: JsonLdOptions,
    private val documentLoader: DocumentLoader,
) : BeanSerializerModifier() {

    override fun modifySerializer(
        config: SerializationConfig?,
        beanDesc: BeanDescription?,
        serializer: JsonSerializer<*>
    ): JsonSerializer<*> {
        if (beanDesc != null && beanDesc.beanClass.isAnnotationPresent(JsonLd::class.java)) {
            return JsonLdSerializer(mapper, options, documentLoader, beanDesc.beanClass)
        }
        return super.modifySerializer(config, beanDesc, serializer)
    }

}