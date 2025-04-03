package de.fraunhofer.iosb.odrl.parser

import com.fasterxml.jackson.databind.module.SimpleModule

class OdrlModule(private val compactSerialization: Boolean) : SimpleModule() {

    override fun setupModule(context: SetupContext) {
        context.addBeanSerializerModifier(OdrlBeanSerializerModifier(compactSerialization))
        context.addBeanDeserializerModifier(OdrlBeanDeserializerModifier())
        super.setupModule(context)
    }

}