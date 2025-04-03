package de.fraunhofer.iosb.odrl.parser

import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.DeserializationConfig
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier
import de.fraunhofer.iosb.odrl.OdrlPolicy

class OdrlBeanDeserializerModifier : BeanDeserializerModifier() {

    override fun modifyDeserializer(
        config: DeserializationConfig?,
        beanDesc: BeanDescription?,
        deserializer: JsonDeserializer<*>
    ): JsonDeserializer<*> {
        val defaultDeserializer = super.modifyDeserializer(config, beanDesc, deserializer)
        if (beanDesc != null && OdrlPolicy::class.java.isAssignableFrom(beanDesc.beanClass)) {
            return OdrlPolicyDeserializer(defaultDeserializer, beanDesc.beanClass)
        }
        return defaultDeserializer
    }

}