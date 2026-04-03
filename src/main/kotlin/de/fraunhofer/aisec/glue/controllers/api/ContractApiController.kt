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
