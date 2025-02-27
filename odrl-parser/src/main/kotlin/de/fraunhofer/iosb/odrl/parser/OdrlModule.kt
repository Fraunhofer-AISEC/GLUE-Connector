package de.fraunhofer.iosb.odrl.parser

import com.fasterxml.jackson.databind.module.SimpleModule

class OdrlModule : SimpleModule() {

    override fun setupModule(context: SetupContext) {
        context.addBeanSerializerModifier(OdrlBeanSerializerModifier())
        context.addBeanDeserializerModifier(OdrlBeanDeserializerModifier())
        super.setupModule(context)
    }

}