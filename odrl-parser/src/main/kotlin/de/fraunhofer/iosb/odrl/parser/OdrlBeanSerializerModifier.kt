package de.fraunhofer.iosb.odrl.parser

import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializationConfig
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier
import de.fraunhofer.iosb.odrl.OdrlAction
import de.fraunhofer.iosb.odrl.OdrlPolicy

class OdrlBeanSerializerModifier(private val compactSerialization: Boolean) : BeanSerializerModifier() {

    override fun modifySerializer(
        config: SerializationConfig?,
        beanDesc: BeanDescription?,
        serializer: JsonSerializer<*>
    ): JsonSerializer<*> {
        val defaultSerializer = super.modifySerializer(config, beanDesc, serializer)
        if (beanDesc != null && OdrlAction::class.java.isAssignableFrom(beanDesc.beanClass)) {
            return OdrlActionSerializer()
        }
        if (compactSerialization) {
            if (beanDesc != null && OdrlPolicy::class.java.isAssignableFrom(beanDesc.beanClass)) {
                return OdrlPolicyCompactingSerializer(defaultSerializer, beanDesc.beanClass)
            }
        }
        return defaultSerializer
    }

}