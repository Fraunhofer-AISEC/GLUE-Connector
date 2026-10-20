/*
 * Copyright 2026 Fraunhofer AISEC
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
package de.fraunhofer.aisec.glue.tables

import com.fasterxml.jackson.databind.ObjectMapper
import de.fraunhofer.aisec.glue.models.negotiation.NegotiationState
import de.fraunhofer.aisec.glue.negotiation.Negotiation.Role
import de.fraunhofer.iosb.odrl.OdrlPolicy
import org.ktorm.jackson.KtormModule
import org.ktorm.jackson.json
import org.ktorm.schema.Table
import org.ktorm.schema.enum
import org.ktorm.schema.int
import org.ktorm.schema.timestamp
import org.ktorm.schema.uuid
import org.ktorm.schema.varchar
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

class Negotiations(
    objectMapper: ObjectMapper,
) : Table<Negotiation>("negotiations") {
    private val ktormObjectMapper = objectMapper.copy().registerModules(KtormModule())

    val id = uuid("id").primaryKey().bindTo { it.id }
    val role = enum<Role>("role").bindTo { it.role }
    val peer = varchar("peer").bindTo { it.peer }
    val peerPid = varchar("peer_pid").bindTo { it.peerPid }
    val startTimestamp =
        timestamp(
            "start_timestamp",
        ).transform({ it.toKotlinInstant() }, { it.toJavaInstant() }).bindTo { it.startTimestamp }
    val state = enum<NegotiationState>("state").bindTo { it.state }
    val callbackUrl = varchar("callback_url").bindTo { it.callbackUrl }
    val peerUrl = varchar("peer_url").bindTo { it.peerUrl }
    val contract = json<OdrlPolicy>("contract", ktormObjectMapper).bindTo { it.contract }
    val failureReason = varchar("failure_reason").bindTo { it.failureReason }
    val lastReceivedMessage = varchar("last_received_message").bindTo { it.lastReceivedMessage }
    val nextSentMessage = int("next_sent_message").bindTo { it.nextSentMessage }
}
