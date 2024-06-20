package de.fraunhofer.aisec.glue.jsonld

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler

class JsonLDProblemHandler : DeserializationProblemHandler() {
    // Don't force our jsonld pojos to declare @id and @type when not needed
    private val allowedMissing = setOf("@id", "@type")

    override fun handleUnknownProperty(
        ctxt: DeserializationContext?,
        p: JsonParser?,
        deserializer: JsonDeserializer<*>?,
        beanOrClass: Any?,
        propertyName: String?
    ): Boolean {
        if (allowedMissing.contains(propertyName)) {
            return true
        }

        return super.handleUnknownProperty(ctxt, p, deserializer, beanOrClass, propertyName)
    }
}