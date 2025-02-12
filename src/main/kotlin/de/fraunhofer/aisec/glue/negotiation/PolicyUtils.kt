package de.fraunhofer.aisec.glue.negotiation

import de.fraunhofer.iosb.odrl.OdrlAgreement
import de.fraunhofer.iosb.odrl.OdrlOffer
import de.fraunhofer.iosb.odrl.OdrlParty
import de.fraunhofer.iosb.odrl.OdrlPolicy
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

fun OdrlPolicy.toOffer(assigner: OdrlParty) = upcast(this, OdrlOffer::class, mapOf(OdrlOffer::assigner.name to assigner))

fun OdrlOffer.toAgreement(assignee: OdrlParty) = upcast(this, OdrlAgreement::class, mapOf(OdrlAgreement::assignee.name to assignee))
