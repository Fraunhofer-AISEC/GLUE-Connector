package de.fraunhofer.aisec.glue.negotiation.builtin

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

class NegotiationContext(val negotiation: BuiltinNegotiation) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<NegotiationContext>
}
