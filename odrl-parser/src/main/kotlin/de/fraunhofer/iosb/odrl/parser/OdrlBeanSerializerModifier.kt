package de.fraunhofer.iosb.odrl.parser

import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializationConfig
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier
import de.fraunhofer.iosb.odrl.OdrlAction

class OdrlBeanSerializerModifier : BeanSerializerModifier() {

    override fun modifySerializer(
        config: SerializationConfig?,
        beanDesc: BeanDescription?,
        serializer: JsonSerializer<*>
    ): JsonSerializer<*> {
        if (beanDesc != null && OdrlAction::class.java.isAssignableFrom(beanDesc.beanClass)) {
            return OdrlActionSerializer()
        }
        return super.modifySerializer(config, beanDesc, serializer)
    }

}