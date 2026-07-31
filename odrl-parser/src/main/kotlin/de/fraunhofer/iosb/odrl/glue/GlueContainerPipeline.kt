package de.fraunhofer.iosb.odrl.glue

import com.fasterxml.jackson.annotation.*
import de.fraunhofer.iosb.odrl.OdrlRightOperand
import de.fraunhofer.iosb.odrl.parser.JsonLdBeanValidator
import io.konform.validation.Validation
import io.konform.validation.ValidationError
import io.konform.validation.jsonschema.minItems

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = GlueContainerPipeline::class)
@JsonTypeName(GlueIdentifiers.CONTAINER_PIPELINE)
data class GlueContainerPipeline(
    @param:JsonProperty(GlueIdentifiers.PLATFORM_SECURITY)
    @param:JsonSetter(nulls = Nulls.AS_EMPTY)
    val platformSecurity: List<String> = listOf(),
    @param:JsonProperty(GlueIdentifiers.HAS_CONTAINER)
    @param:JsonSetter(nulls = Nulls.AS_EMPTY)
    val container: List<GlueContainer> = listOf(),
    @param:JsonProperty(GlueIdentifiers.ATLS_PROXY_IN)
    val atlsProxyIn: GlueAtlsProxyConfiguration? = null,
    @param:JsonProperty(GlueIdentifiers.ATLS_PROXY_OUT)
    val atlsProxyOut: GlueAtlsProxyConfiguration? = null,
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