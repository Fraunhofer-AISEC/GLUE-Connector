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

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import de.fraunhofer.iosb.odrl.parser.JsonLdBeanValidator
import io.konform.validation.Validation
import io.konform.validation.ValidationError
import java.net.URI

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonTypeName(OdrlIdentifiers.AGREEMENT)
open class OdrlAgreement(
    // Properties of superclass OdrlPolicy
    uid: URI,
    permission: List<OdrlPermission> = listOf(),
    prohibition: List<OdrlProhibition> = listOf(),
    obligation: List<OdrlDuty> = listOf(),
    profile: List<OdrlIri> = listOf(),
    inheritFrom: List<OdrlIri> = listOf(),
    conflict: OdrlConflictTerm? = null,
) : OdrlPolicy(uid, permission, prohibition, obligation, profile, inheritFrom, conflict),
    JsonLdBeanValidator {
    override fun validate(): List<ValidationError> = validateOdrlAgreement.validate(this).errors

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        return true
    }

    override fun hashCode(): Int = super.hashCode()

    override fun toString(): String = "OdrlAgreement(${getPropertyMap().entries.joinToString(", ")})"
}

val validateOdrlAgreement =
    Validation {
        run(validateOdrlPolicy)

        // An Agreement MUST have one assigner property value in each rule
        // An Agreement MUST have one assignee property value in each rule
        OdrlAgreement::permission onEach {
            OdrlPermission::assignee required {}
            OdrlPermission::assigner required {}
        }
        OdrlAgreement::prohibition onEach {
            OdrlProhibition::assignee required {}
            OdrlProhibition::assigner required {}
        }
        OdrlAgreement::obligation onEach {
            OdrlDuty::assignee required {}
            OdrlDuty::assigner required {}
        }
    }
