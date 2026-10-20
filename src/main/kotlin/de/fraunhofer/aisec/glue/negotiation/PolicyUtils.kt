/*
 * Copyright 2025-2026 Fraunhofer AISEC
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
package de.fraunhofer.aisec.glue.negotiation

import de.fraunhofer.iosb.odrl.OdrlDuty
import de.fraunhofer.iosb.odrl.OdrlOffer
import de.fraunhofer.iosb.odrl.OdrlParty
import de.fraunhofer.iosb.odrl.OdrlPermission
import de.fraunhofer.iosb.odrl.OdrlPolicy
import de.fraunhofer.iosb.odrl.OdrlProhibition
import de.fraunhofer.iosb.odrl.parser.OdrlParser
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.primaryConstructor

fun <T : Any, R : Any> upcast(
    baseObj: T,
    targetClass: KClass<R>,
    additionalParams: Map<String, Any> = emptyMap(),
): R {
    val baseProperties = baseObj::class.members.filterIsInstance<KProperty1<T, *>>()
    val constructor =
        targetClass.primaryConstructor
            ?: throw IllegalArgumentException("Could not upcast $baseObj to $targetClass, no primary constructor")
    return constructor.callBy(
        constructor.parameters.associateWith { param ->
            val pName = param.name
            if (pName in additionalParams) {
                additionalParams[pName]
            } else {
                try {
                    baseProperties.first { it.name == pName }.get(baseObj)
                } catch (_: NoSuchElementException) {
                    throw IllegalArgumentException("Could not upcast $baseObj to $targetClass, missing parameter for $pName")
                }
            }
        },
    )
}

fun OdrlPolicy.toOffer(
    parser: OdrlParser,
    assigner: OdrlParty,
) = parser.convertToOdrlOffer(this, assigners = listOf(assigner))

fun OdrlOffer.toAgreement(
    parser: OdrlParser,
    assignee: OdrlParty,
) = parser.convertToOdrlAgreement(this, assignees = listOf(assignee))

fun OdrlPermission.resetParties(
    assignee: OdrlParty? = null,
    assigner: OdrlParty? = null,
): OdrlPermission =
    OdrlPermission(
        uid = this.uid,
        action = this.action,
        target = this.target,
        assignee = assignee,
        assigner = assigner,
        attributedParty = this.attributedParty,
        attributingParty = this.attributingParty,
        compensatedParty = this.compensatedParty,
        compensatingParty = this.compensatingParty,
        consentedParty = this.consentedParty,
        consentingParty = this.consentingParty,
        contractedParty = this.contractedParty,
        contractingParty = this.contractingParty,
        informedParty = this.informedParty,
        informingParty = this.informingParty,
        trackedParty = this.trackedParty,
        trackingParty = this.trackingParty,
        constraint = this.constraint,
        duty = this.duty.map { it.resetParties(assignee, assigner) },
    )

fun OdrlProhibition.resetParties(
    assignee: OdrlParty? = null,
    assigner: OdrlParty? = null,
): OdrlProhibition =
    OdrlProhibition(
        uid = this.uid,
        action = this.action,
        target = this.target,
        assignee = assignee,
        assigner = assigner,
        attributedParty = this.attributedParty,
        attributingParty = this.attributingParty,
        compensatedParty = this.compensatedParty,
        compensatingParty = this.compensatingParty,
        consentedParty = this.consentedParty,
        consentingParty = this.consentingParty,
        contractedParty = this.contractedParty,
        contractingParty = this.contractingParty,
        informedParty = this.informedParty,
        informingParty = this.informingParty,
        trackedParty = this.trackedParty,
        trackingParty = this.trackingParty,
        constraint = this.constraint,
        remedy = this.remedy.map { it.resetParties(assignee, assigner) },
    )

fun OdrlDuty.resetParties(
    assignee: OdrlParty? = null,
    assigner: OdrlParty? = null,
): OdrlDuty =
    OdrlDuty(
        uid = this.uid,
        action = this.action,
        target = this.target,
        assignee = assignee,
        assigner = assigner,
        attributedParty = this.attributedParty,
        attributingParty = this.attributingParty,
        compensatedParty = this.compensatedParty,
        compensatingParty = this.compensatingParty,
        consentedParty = this.consentedParty,
        consentingParty = this.consentingParty,
        contractedParty = this.contractedParty,
        contractingParty = this.contractingParty,
        informedParty = this.informedParty,
        informingParty = this.informingParty,
        trackedParty = this.trackedParty,
        trackingParty = this.trackingParty,
        constraint = this.constraint,
        consequence = this.consequence.map { it.resetParties(assignee, assigner) },
    )
