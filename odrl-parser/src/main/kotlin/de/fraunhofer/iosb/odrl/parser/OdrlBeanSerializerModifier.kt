package de.fraunhofer.iosb.odrl.parser

import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializationConfig
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier

class OdrlBeanSerializerModifier : BeanSerializerModifier() {

    override fun modifySerializer(
        config: SerializationConfig?,
        beanDesc: BeanDescription?,
        serializer: JsonSerializer<*>
    ): JsonSerializer<*> {
        return super.modifySerializer(config, beanDesc, serializer)
    }

}