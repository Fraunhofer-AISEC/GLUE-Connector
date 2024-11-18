package fhg.glue.odrl.de.fraunhofer.iosb.odrl

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import fhg.glue.odrl.de.fraunhofer.iosb.odrl.glue.GlueIdentifiers
import fhg.glue.odrl.de.fraunhofer.iosb.odrl.parser.JsonLd

@JsonLd(GlueIdentifiers.CONTEXT)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = OdrlConstraint::class)
@JsonSubTypes(
    Type(OdrlConstraint::class),
    Type(OdrlLogicalConstraint::class),
)
interface OdrlConstraintRepresentation