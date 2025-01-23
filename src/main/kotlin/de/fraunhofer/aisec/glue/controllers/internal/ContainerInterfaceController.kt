package de.fraunhofer.aisec.glue.controllers.internal

import de.fraunhofer.aisec.glue.Utils
import de.fraunhofer.aisec.glue.controllers.Controller
import de.fraunhofer.aisec.glue.negotiation.NegotiationManager
import de.fraunhofer.aisec.glue.policy.PolicyEngine
import io.ktor.http.*
import io.ktor.server.plugins.*
import io.ktor.server.routing.*

/**
 * This controller represents the interface
 */
class ContainerInterfaceController(val negotiationManager: NegotiationManager, val policyEngine: PolicyEngine) : Controller {
    override fun Route.registerRoutes() {
        // TOOD: Insert some container-side authentication
        // Just IP-Address seems like it *could* be enough here (containers cannot really fake that as they cannot
        // That said, we could easily enough just inject a token for the container to authenticate with.
        route("/coordinator") {
            // Add a value for a pre-defined policy parameter
            post("/policy-parameter") {
                //  We can obtain the associated pipeline by its IP (see route-level comments for caveats)
            }

            post("/proxy") {
                val destination = call.queryParameters["destination"]
                val port = call.queryParameters["port"]

                val originUrl = Utils.baseUrlForOrigin(call.request.origin, 2)
                val callbackUrl = URLBuilder(originUrl).appendPathSegments("negotiations").build()
                val negotiation =
                    negotiationManager.startNegotiation(
                        Url("http://$destination:8080/negotiations"),
                        callbackUrl,
                        TODO("Somehow instantiate the policy template"),
                    )
                val newPolicy = negotiation.await()

                //  with appropriate contract id and action/target

                //  parameter maps to `containerPort` in the response).
            }
        }
    }
}
