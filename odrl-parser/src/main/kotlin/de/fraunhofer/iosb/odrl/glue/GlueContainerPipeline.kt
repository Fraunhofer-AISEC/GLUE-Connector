package de.fraunhofer.iosb.odrl.glue

import com.fasterxml.jackson.annotation.*
import de.fraunhofer.iosb.odrl.OdrlRightOperand
import de.fraunhofer.iosb.odrl.parser.JsonLd
import de.fraunhofer.iosb.odrl.parser.JsonLdBeanValidator
import io.konform.validation.Validation
import io.konform.validation.ValidationError
import io.konform.validation.jsonschema.minItems

@JsonLd(GlueIdentifiers.CONTEXT)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = GlueContainerPipeline::class)
@JsonTypeName(GlueIdentifiers.CONTAINER_PIPELINE)
data class GlueContainerPipeline(
    @JsonProperty(GlueIdentifiers.PLATFORM_SECURITY)
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    val platformSecurity: List<String> = listOf(),
    @JsonProperty(GlueIdentifiers.HAS_CONTAINER)
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    val container: List<GlueContainer> = listOf(),
) : OdrlRightOperand(), JsonLdBeanValidator {
    override fun validate(): List<ValidationError> {
        return validateGlueContainerPipeline.validate(this).errors
    }

    override fun compareTo(other: OdrlRightOperand): Int {
        throw UnsupportedOperationException("Cannot compare values of type GlueContainerPipeline")
    }

    override fun toString(): String {
        return "GlueContainerPipeline(platform=${platformSecurity}, container=[${container.joinToString(" -> ")}])"
    }
}

val validateGlueContainerPipeline = Validation {
    GlueContainerPipeline::platformSecurity onEach {
        addConstraint("GlueContainerPipeline: {value} must be one of 'tpm2', 'sgx', 'tdx' or 'amd-sev'") {
            listOf("tpm2", "sgx", "tdx", "amd-sev").any { it2 -> it2.equals(it, false) }
        }
    }

    GlueContainerPipeline::container {
        minItems(1)
    }

    GlueContainerPipeline::container onEach {
        run(validateGlueContainer)
    }
}