package de.fraunhofer.aisec.glue.negotiation

import de.fraunhofer.iosb.odrl.OdrlPolicy

typealias NegotiationCompletionCallback = suspend (result: Result<OdrlPolicy>) -> Unit
