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
package de.fraunhofer.aisec.glue.controllers.api

import de.fraunhofer.aisec.glue.DataStore
import de.fraunhofer.aisec.glue.controllers.Controller
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * This controller allows a user to enumerate negotiated contracts/agreements stored in the data store.
 */
class ContractApiController(
    private val dataStore: DataStore,
) : Controller {
    override fun Route.registerRoutes() {
        route(ApiConstants.API_BASE_PATH) {
            route("/contracts") {
                get {
                    call.respond(dataStore.contracts())
                }
            }
        }
    }
}
