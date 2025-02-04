package de.fraunhofer.aisec.glue.controllers.internal

import com.fasterxml.jackson.databind.JsonNode
import de.fraunhofer.aisec.glue.ContractStore
import de.fraunhofer.aisec.glue.Utils
import de.fraunhofer.aisec.glue.controllers.Controller
import de.fraunhofer.aisec.glue.negotiation.NegotiationManager
import de.fraunhofer.aisec.glue.pipeline.PipelineManager
import de.fraunhofer.aisec.glue.policy.PolicyEngine
import de.fraunhofer.iosb.odrl.OdrlIdentifiers
import de.fraunhofer.iosb.odrl.glue.GlueValueProcessor
import io.ktor.http.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.net.URI

/**
 * This controller represents the interface
 */
class ContainerInterfaceController(val negotiationManager: NegotiationManager, val pipelineManager: PipelineManager, val contractStore: ContractStore, val policyEngine: PolicyEngine) : Controller {
    override fun Route.registerRoutes() {
        // TOOD: Insert some container-side authentication
        // Just IP-Address seems like it *could* be enough here (containers cannot really fake that as they cannot
        // send raw packets (TODO: is there any other way to fake the source address that work in containers?).
        // That said, we could easily enough just inject a token for the container to authenticate with.
        route("/coordinator") {
            // Add a value for a pre-defined policy parameter
            post("/policy-parameter") {
                // TODO(long-term): Add some validation to detect if the container is allowed to supply that parameter
                val parameters = call.receive<Map<String, JsonNode>>()
                val pipeline = pipelineManager.getPipelineByIp(call.request.origin.remoteAddress)
                pipeline?.placeholderValues?.putAll(parameters) ?: call.respond(HttpStatusCode.NotFound)
            }

            post("/proxy") {
                val destination = call.queryParameters["destination"]
                val port = call.queryParameters["port"]
                // TODO(long-term?): We also need to check for target here since a policy could have more than one

                val pipeline = pipelineManager.getPipelineByIp(call.request.origin.remoteAddress)
                if (pipeline == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@post
                }

                val contract = contractStore.getContract(pipeline.contractId)
                if (contract == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@post
                }

                val evaluationResult = policyEngine.evaluatePolicy(contract, URI(OdrlIdentifiers.DISTRIBUTE), URI(pipeline.targetId))
                val evaluation = evaluationResult.getOrElse {
                    call.respond(HttpStatusCode.InternalServerError)
                    return@post
                }

                if (evaluation.nextPolicyTemplate == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@post
                }

                val nextPolicy = try {
                    GlueValueProcessor().resolvePlaceholders(evaluation.nextPolicyTemplate) {
                        pipeline.placeholderValues[it] ?: throw NullPointerException("No value for placeholder $it")
                    }
                } catch (_: NullPointerException) {
                    call.respond(HttpStatusCode.NotFound)
                    return@post
                }

                val originUrl = Utils.baseUrlForOrigin(call.request.origin, 2)
                val callbackUrl = URLBuilder(originUrl).appendPathSegments("negotiations").build()
                // TODO: Check if we already have an agreement with the destination node
                val negotiation =
                    negotiationManager.startNegotiation(
                        Url("http://$destination:8080/negotiations"),
                        callbackUrl,
                        TODO("Somehow instantiate the policy template"),
                    )
                val newPolicy = negotiation.await()

                // TODO: Use the new policy to start a pipeline on the destination using the `POST /pipeline` endpoint
                //  with appropriate contract id and action/target

                // TODO: Use the portmap of the response to determine the destination port (this request's port
                //  parameter maps to `containerPort` in the response).
            }
        }
    }
}
