/*
 * Copyright 2024-2026 Fraunhofer AISEC
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
package de.fraunhofer.aisec.glue.negotiation.builtin

import de.fraunhofer.iosb.odrl.OdrlOffer
import org.slf4j.LoggerFactory

class DummyNegotiator(
    disableWarning: Boolean = false,
) : Negotiator {
    init {
        if (!disableWarning) {
            LOG.warn("Using placeholder dummy negotiator, which should be replaced ASAP")
        }
    }

    override fun evaluateOffer(contractOffer: OdrlOffer): Pair<Boolean, OdrlOffer?> = Pair(true, null)

    override fun evaluateRequest(contractRequest: OdrlOffer): Pair<Boolean, OdrlOffer?> = Pair(true, null)

    companion object {
        private val LOG = LoggerFactory.getLogger(DummyNegotiator::class.java)
    }
}
