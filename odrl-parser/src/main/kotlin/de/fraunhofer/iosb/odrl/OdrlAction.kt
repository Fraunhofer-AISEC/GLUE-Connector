package de.fraunhofer.iosb.odrl

import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.databind.type.CollectionType
import de.fraunhofer.iosb.odrl.glue.GlueIdentifiers
import de.fraunhofer.iosb.odrl.parser.JsonLd
import de.fraunhofer.iosb.odrl.parser.JsonLdBeanValidator
import io.konform.validation.Validation
import io.konform.validation.ValidationError

@JsonLd(GlueIdentifiers.CONTEXT)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = OdrlAction::class)
@JsonTypeName(OdrlIdentifiers.ACTION)
@JsonDeserialize(using = OdrlActionDeserializer::class)
data class OdrlAction(
    // According to the ODRL information model, an action must be identified by a rdf:value when using refinements
    @param:JsonProperty(RdfIdentifiers.VALUE)
    val value: RdfValue,

    @param:JsonProperty(OdrlIdentifiers.REFINEMENT)
    @param:JsonSetter(nulls = Nulls.AS_EMPTY)
    val refinement: List<OdrlConstraintRepresentation> = listOf(),

    @param:JsonProperty(OdrlIdentifiers.INCLUDED_IN)
    val includedIn: OdrlAction? = null,

    @param:JsonProperty(OdrlIdentifiers.IMPLIES)
    @param:JsonSetter(nulls = Nulls.AS_EMPTY)
    val implies: List<OdrlAction> = listOf(),
) : JsonLdBeanValidator {
    // Constructor necessary for Jackson to deserialize primitive type
    @Suppress("unused")
    constructor(primitiveValue: String) : this(RdfValue(primitiveValue))

    override fun validate(): List<ValidationError> {
        return validateOdrlAction.validate(this).errors
    }

    private fun getPropertyMap(): MutableMap<String, Any> {
        val map = mutableMapOf<String, Any>()
        map["value"] = value
        if (refinement.isNotEmpty()) map["refinement"] = refinement
        if (includedIn != null) map["includedIn"] = includedIn
        if (implies.isNotEmpty()) map["implies"] = implies
        return map
    }

    override fun toString(): String {
        return "OdrlAction(${getPropertyMap().entries.joinToString(", ")})"
    }
}

val validateOdrlAction = Validation {
    OdrlAction::value {
        addConstraint("OdrlAction: {value} is not a valid IRI") { isIri(it.uid) }
    }

    // An Action MAY have none, one or more refinement property values of type Constraint
    OdrlAction::refinement {
        addConstraint("OdrlAction: Refinement list must be of homogeneous types Constraint/LogicalConstraint") { list ->
            list.all { it is OdrlConstraint } || list.all { it is OdrlLogicalConstraint }
        }
    }
    OdrlAction::refinement onEach {
        ifInstanceOf<OdrlConstraint> {
            run(validateOdrlConstraint)
        }
        ifInstanceOf<OdrlLogicalConstraint> {
            run(validateOdrlLogicalConstraint)
        }
    }

    // An Action (except for use and transfer) MUST have one includedIn property value of type Action
    // Note: This property may be defined in the vocabulary instead of the JSONLD object
    OdrlAction::includedIn ifPresent {
        addConstraint("OdrlAction: Invalid action in property 'includedIn'") { validateRecursively(it) }
    }

    // An Action MAY have none, one or more implies property values of type Action
    OdrlAction::implies onEach {
        addConstraint("OdrlAction: Invalid action in property 'implies'") { validateRecursively(it) }
    }
}

private fun validateRecursively(action: OdrlAction): Boolean {
    return validateOdrlAction.validate(action).isValid
}

class OdrlActionDeserializer : JsonDeserializer<OdrlAction>() {
    // Helper to unwrap a single-level HAS_ACTION container
    fun JsonNode.unwrapAction(): JsonNode =
        this.get(OdrlIdentifiers.HAS_ACTION)?.let { actionNode ->
            if (actionNode.isArray && actionNode.size() == 1) actionNode[0] else actionNode
        } ?: this

    /**
     * Deserializes `OdrlAction` from JSON, handling various formats.
     * Mostly takes care of unwrapping EDC-specific non-standard structures.
     */
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): OdrlAction {
        val mapper = p.codec as ObjectMapper
        val node: JsonNode = mapper.readTree(p)

        // Handle the simple string case: "action": "use"
        if (node.isTextual) {
            return OdrlAction(node.asText())
        }

        // Handle object case with rdf:value, @id, or nested action object
        // Find and unwrap the value node in one step
        val valueNode = (node.get(RdfIdentifiers.VALUE)
            ?: node.get("@id")
            ?: node.get(OdrlIdentifiers.HAS_ACTION)
            ?: throw MismatchedInputException.from(
                p,
                OdrlAction::class.java,
                "Missing '${RdfIdentifiers.VALUE}', '@id', or '${OdrlIdentifiers.HAS_ACTION}' property"
            )).unwrapAction()

        val value = when {
            valueNode.isTextual -> RdfValue(valueNode.asText())
            valueNode.isObject && valueNode.has("@id") -> mapper.treeToValue(valueNode, RdfValue::class.java)
            else -> throw MismatchedInputException.from(p, RdfValue::class.java, "Invalid value format: $valueNode")
        }

        // Deserialize other properties
        val refinement: List<OdrlConstraintRepresentation> = node.get(OdrlIdentifiers.REFINEMENT)?.let { refinementNode ->
            val listType: CollectionType = mapper.typeFactory.constructCollectionType(
                List::class.java,
                OdrlConstraintRepresentation::class.java
            )
            mapper.treeToValue(refinementNode, listType)
        } ?: listOf()

        val includedIn: OdrlAction? = node.get(OdrlIdentifiers.INCLUDED_IN)?.let {
            mapper.treeToValue(it, OdrlAction::class.java)
        }

        val implies: List<OdrlAction> = node.get(OdrlIdentifiers.IMPLIES)?.let { impliesNode ->
            val listType: CollectionType = mapper.typeFactory.constructCollectionType(
                List::class.java,
                OdrlAction::class.java
            )
            mapper.treeToValue(impliesNode, listType)
        } ?: listOf()

        return OdrlAction(value, refinement, includedIn, implies)
    }
}
