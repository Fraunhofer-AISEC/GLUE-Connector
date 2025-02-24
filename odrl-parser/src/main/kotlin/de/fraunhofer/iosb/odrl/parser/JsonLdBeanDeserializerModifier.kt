package de.fraunhofer.iosb.odrl.parser

import com.apicatalog.jsonld.JsonLdOptions
import com.apicatalog.jsonld.loader.DocumentLoader
import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.DeserializationConfig
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier
import de.fraunhofer.iosb.odrl.OdrlPolicy

class JsonLdBeanDeserializerModifier(
    private val mapper: ObjectMapper,
    private val options: JsonLdOptions,
    private val documentLoader: DocumentLoader,
) : BeanDeserializerModifier() {

    override fun modifyDeserializer(
        config: DeserializationConfig?,
        beanDesc: BeanDescription?,
        deserializer: JsonDeserializer<*>
    ): JsonDeserializer<*> {
        if (beanDesc != null && OdrlPolicy::class.java.isAssignableFrom(beanDesc.beanClass)) {
            return JsonLdDeserializer(OdrlPolicyMapper(mapper, beanDesc.beanClass), options, documentLoader)
        }
        if (beanDesc != null && beanDesc.beanClass.isAnnotationPresent(JsonLd::class.java)) {
            return JsonLdDeserializer(JsonLdMapper(mapper, beanDesc.beanClass), options, documentLoader)
        }
        return super.modifyDeserializer(config, beanDesc, deserializer)
    }

}