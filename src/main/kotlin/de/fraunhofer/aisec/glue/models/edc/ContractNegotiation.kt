package de.fraunhofer.aisec.glue.models.edc

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import de.fraunhofer.iosb.odrl.parser.JsonLd
import java.net.URI

@JsonLd(Contexts.EDC)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonTypeName("${Namespaces.EDC}/ContractNegotiation")
data class ContractNegotiation(
    @JsonProperty("@id")
    val uid: URI,
    // The correct type is currently not mapped to any object
    @JsonProperty("${Namespaces.EDC}/callbackAddresses")
    val callbackAddresses: List<Any>,
    @JsonProperty("${Namespaces.EDC}/contractAgreementId")
    val contractAgreementId: URI? = null,
    @JsonProperty("${Namespaces.EDC}/counterPartyAddress")
    val counterPartyAddress: String,
    @JsonProperty("${Namespaces.EDC}/counterPartyId")
    val counterPartyId: URI,
    @JsonProperty("${Namespaces.EDC}/errorDetail")
    val errorDetail: String? = null,
    @JsonProperty("${Namespaces.EDC}/protocol")
    val protocol: String,
    @JsonProperty("${Namespaces.EDC}/state")
    val state: NegotiationState,
    @JsonProperty("${Namespaces.EDC}/type")
    val type: NegotiationType,
    @JsonProperty("${Namespaces.EDC}/createdAt")
    val createdAt: Long,
) {
    enum class NegotiationState {
        INITIAL,
        REQUESTING,
        REQUESTED,
        OFFERING,
        OFFERED,
        ACCEPTING,
        ACCEPTED,
        AGREEING,
        AGREED,
        VERIFYING,
        VERIFIED,
        FINALIZING,
        FINALIZED,
        TERMINATING,
        TERMINATED,
    }

    enum class NegotiationType {
        CONSUMER,
        PROVIDER,
    }
}
