/*
 * Copyright 2024 Fraunhofer AISEC
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
import de.fraunhofer.iosb.odrl.OdrlPolicy
import org.ktorm.jackson.KtormModule
import org.ktorm.jackson.json
import org.ktorm.schema.Table
import org.ktorm.schema.varchar

class Contracts(
    objectMapper: ObjectMapper,
) : Table<Contract>("contracts") {
    val id = varchar("id").primaryKey().bindTo { it.id }
    val contract =
        json<OdrlPolicy>("contract", objectMapper.copy().registerModules(KtormModule()))
            .bindTo { it.contract }
}
