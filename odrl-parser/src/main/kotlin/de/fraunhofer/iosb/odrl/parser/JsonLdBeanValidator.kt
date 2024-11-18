package fhg.glue.odrl.de.fraunhofer.iosb.odrl.parser

import io.konform.validation.ValidationError

interface JsonLdBeanValidator {
    fun validate(): List<ValidationError>
}