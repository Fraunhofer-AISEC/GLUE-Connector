package de.fraunhofer.iosb.odrl.parser

import com.apicatalog.jsonld.JsonLdOptions
import com.apicatalog.jsonld.loader.DocumentLoader
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.databind.util.StdDateFormat
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import de.fraunhofer.iosb.odrl.*
import de.fraunhofer.iosb.odrl.parser.JsonLdModule.Companion.createDocumentLoader
import java.io.Reader
import java.net.URI

@Suppress("unused")
class OdrlParser(
    val mapper: ObjectMapper = ObjectMapper(),
    compactSerialization: Boolean = false,
    options: JsonLdOptions = JsonLdOptions(),
    documentLoader: DocumentLoader = createDocumentLoader(),
) {

    private val odrlMapper: ObjectMapper

    init {
        mapper.registerKotlinModule()
        odrlMapper = mapper.copy()
            .registerModule(OdrlModule(compactSerialization))
            .registerModule(JavaTimeModule())
            .setDateFormat(StdDateFormat().withColonInTimeZone(true)) // Provides ISO time support
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)    // Hides empty properties in serialized policies
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY) // Accepts single properties as list
        mapper.registerModule(JsonLdModule(odrlMapper, options, documentLoader))
    }

    /**
     * Deserialize a policy.
     * @param r Input to deserialize.
     * @return The policy.
     */
    fun readOdrlPolicy(r: Reader): OdrlPolicy {
        return mapper.readValue(r, OdrlPolicy::class.java)
    }

    /**
     * Serialize a policy.
     * @param policy The policy to serialize.
     * @return The policy serialized as string.
     */
    fun writeOdrlPolicy(policy: OdrlPolicy): String {
        return mapper.writeValueAsString(policy)
    }

    /**
     * Try to convert a value to a policy.
     * @param value The value to convert.
     * @return The converted policy.
     * @throws IllegalArgumentException if the conversion failed.
     */
    fun convertToOdrlPolicy(value: Any): OdrlPolicy {
        return mapper.convertValue(value, OdrlPolicy::class.java)
    }

    /**
     * Convert a policy to a set.
     * @param policy The policy to convert.
     * @param newUid If specified, replaces the policy uid in the converted offer.
     * @return The converted set.
     */
    fun convertToOdrlSet(
        policy: OdrlPolicy,
        newUid: URI? = null,
    ): OdrlSet {
        return convertOdrlPolicy(policy, OdrlIdentifiers.SET, null, null, newUid)
    }

    /**
     * Try to convert a policy to an offer.
     * @param policy The policy to convert.
     * @param assignees If specified, replaces the assignees in the converted offer.
     * @param assigners If specified, replaces the assigners in the converted offer.
     * @param newUid If specified, replaces the policy uid in the converted offer.
     * @throws IllegalArgumentException if no assigners are found.
     * @return The converted offer.
     */
    fun convertToOdrlOffer(
        policy: OdrlPolicy,
        assignees: List<OdrlParty>? = null,
        assigners: List<OdrlParty>? = null,
        newUid: URI? = null,
    ): OdrlOffer {
        return convertOdrlPolicy(policy, OdrlIdentifiers.OFFER, assignees, assigners, newUid)
    }

    /**
     * Convert a policy to a fresh offer (i.e. w/o assignees) for the specified assigner.
     * @param policy The policy to convert.
     * @param assigner Assigner of the offer.
     * @param newUid If specified, replaces the policy uid in the converted offer.
     * @return The converted offer.
     */
    fun convertToOdrlOffer(
        policy: OdrlPolicy,
        assigner: OdrlParty,
        newUid: URI? = null,
    ) = convertToOdrlOffer(policy, assignees = listOf(), assigners = listOf(assigner), newUid)

    /**
     * Try to convert a policy to an agreement.
     * @param policy The policy to convert.
     * @param assignees If specified, replaces the assignees in the agreement.
     * @param assigners If specified, replaces the assigners in the agreement.
     * @param newUid If specified, replaces the policy uid in the converted agreement.
     * @throws IllegalArgumentException if no assignees or no assigners are found.
     * @return The converted agreement.
     */
    fun convertToOdrlAgreement(
        policy: OdrlPolicy,
        assignees: List<OdrlParty>? = null,
        assigners: List<OdrlParty>? = null,
        newUid: URI? = null,
    ): OdrlAgreement {
        return convertOdrlPolicy(policy, OdrlIdentifiers.AGREEMENT, assignees, assigners, newUid)
    }

    /**
     * Convert a policy to an agreement for the specified assignee and assigner.
     * @param policy The policy to convert.
     * @param assignee Assignee of the agreement.
     * @param assigner Assigner of the agreement.
     * @param newUid If specified, replaces the policy uid in the converted agreement.
     * @return The converted agreement.
     */
    fun convertToOdrlAgreement(
        policy: OdrlPolicy,
        assignee: OdrlParty,
        assigner: OdrlParty,
        newUid: URI? = null,
    ) = convertToOdrlAgreement(policy, assignees = listOf(assignee), assigners = listOf(assigner), newUid)

    /**
     * Convert an offer to an agreement for the specified assignee. The offer's assigner is retained.
     * @param assignee Assignee of the agreement.
     * @param newUid If specified, replaces the offer uid in the converted agreement.
     * @return The converted agreement.
     */
    fun convertToOdrlAgreement(
        offer: OdrlOffer,
        assignee: OdrlParty,
        newUid: URI? = null,
    ) = convertToOdrlAgreement(offer, assignees = listOf(assignee), assigners = null, newUid)

    private fun <T : OdrlPolicy> convertOdrlPolicy(
        policy: OdrlPolicy,
        type: String,
        assignees: List<OdrlParty>?,
        assigners: List<OdrlParty>?,
        newUid: URI?,
    ): T {
        val policyNode = odrlMapper.convertValue(policy, ObjectNode::class.java)
        policyNode.replace("@type", TextNode(type))
        if (newUid != null)
            policyNode.replace("@id", TextNode(newUid.toString()))

        val processor = OdrlRuleCompositionProcessor()
        if (assignees != null) {
            val assigneeNodes = assignees.map { odrlMapper.convertValue(it, ObjectNode::class.java) }
            processor.replacePropertyInOdrlRules(policyNode, OdrlIdentifiers.ASSIGNEE, assigneeNodes)
        }
        if (assigners != null) {
            val assignerNodes = assigners.map { odrlMapper.convertValue(it, ObjectNode::class.java) }
            processor.replacePropertyInOdrlRules(policyNode, OdrlIdentifiers.ASSIGNER, assignerNodes)
        }

        val bean = odrlMapper.convertValue(policyNode, object : TypeReference<T>() {})
        val validationErrors = bean.validate()
        if (validationErrors.isNotEmpty())
            throw JsonMappingException(null, validationErrors.toString())
        return bean
    }
}