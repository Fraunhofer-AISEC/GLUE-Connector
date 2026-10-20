/*
 * Copyright 2024-2026 Fraunhofer AISEC and Fraunhofer IOSB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.fraunhofer.iosb.odrl

import com.fasterxml.jackson.annotation.*
import de.fraunhofer.iosb.odrl.parser.JsonLdBeanValidator
import io.konform.validation.Validation
import io.konform.validation.ValidationError
import java.net.URI

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = OdrlDuty::class)
@JsonTypeName(OdrlIdentifiers.DUTY)
class OdrlDuty(
    // Properties of superclass OdrlRule
    uid: URI? = null,
    action: OdrlAction,
    target: OdrlAsset? = null,
    output: OdrlAsset? = null,
    assignee: OdrlParty? = null,
    assigner: OdrlParty? = null,
    attributedParty: OdrlParty? = null,
    attributingParty: OdrlParty? = null,
    compensatedParty: OdrlParty? = null,
    compensatingParty: OdrlParty? = null,
    consentedParty: OdrlParty? = null,
    consentingParty: OdrlParty? = null,
    contractedParty: OdrlParty? = null,
    contractingParty: OdrlParty? = null,
    informedParty: OdrlParty? = null,
    informingParty: OdrlParty? = null,
    trackedParty: OdrlParty? = null,
    trackingParty: OdrlParty? = null,
    constraint: List<OdrlConstraintRepresentation> = listOf(),
    // Properties of OdrlDuty
    @param:JsonProperty(OdrlIdentifiers.CONSEQUENCE)
    @param:JsonSetter(nulls = Nulls.AS_EMPTY)
    val consequence: List<OdrlDuty> = listOf(),
) : OdrlRule(
        uid,
        action,
        target,
        output,
        assignee,
        assigner,
        attributedParty,
        attributingParty,
        compensatedParty,
        compensatingParty,
        consentedParty,
        consentingParty,
        contractedParty,
        contractingParty,
        informedParty,
        informingParty,
        trackedParty,
        trackingParty,
        constraint,
    ),
    JsonLdBeanValidator {
    override fun validate(): List<ValidationError> = validateOdrlDuty.validate(this).errors

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as OdrlDuty

        return consequence == other.consequence
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + consequence.hashCode()
        return result
    }

    override fun getPropertyMap(): MutableMap<String, Any> {
        val map = super.getPropertyMap()
        if (consequence.isNotEmpty()) map["consequence"] = consequence
        return map
    }

    override fun toString(): String = "OdrlDuty(${getPropertyMap().entries.joinToString(", ")})"
}

val validateOdrlDuty =
    Validation {
        run(validateOdrlRule)

        // A Duty MAY have none or one target property values (of type Asset)
        // Other relation sub-properties MAY be used
        // Note: Already validated as part of Rule

        // A Duty MAY have none or one assigner and/or assignee property values of type Party
        // Other function sub-properties MAY be used
        // Note: Already validated as part of Rule

        // A Duty MAY have none, one or many consequence property values of type Duty
        OdrlDuty::consequence onEach {
            addConstraint("OdrlDuty: Invalid duty in property 'consequence'") { validateRecursively(it) }
        }
    }

private fun validateRecursively(duty: OdrlDuty): Boolean = validateOdrlDuty.validate(duty).isValid
