package fhg.glue.odrl.de.fraunhofer.iosb.odrl.parser

import com.apicatalog.jsonld.JsonLdOptions
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule

class JsonLdModule (
    private val mapper: ObjectMapper = ObjectMapper(),
    private val options: JsonLdOptions = JsonLdOptions(),
) : SimpleModule() {
    override fun setupModule(context: SetupContext) {
        context.addBeanSerializerModifier(JsonLdBeanSerializerModifier(mapper, options))
        context.addBeanDeserializerModifier(JsonLdBeanDeserializerModifier(mapper, options))
        super.setupModule(context)
    }
}