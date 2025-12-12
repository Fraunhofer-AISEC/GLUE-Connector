package de.fraunhofer.aisec.glue.controllers

import de.fraunhofer.aisec.glue.ContractStore
import de.fraunhofer.aisec.glue.models.pipeline.PipelineCreateRequest
import de.fraunhofer.aisec.glue.models.pipeline.PipelineCreateResponse
import de.fraunhofer.aisec.glue.pipeline.PipelineManager
import de.fraunhofer.aisec.glue.policy.PolicyEngine
import de.fraunhofer.aisec.glue.policy.PolicyViolationException
import de.fraunhofer.iosb.odrl.OdrlIdentifiers
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.net.URI

/**
 * This controller provides an interface for other connectors to manage container pipelines associated with their
 * policies.
 */
class PipelineController(
    private val contractStore: ContractStore,
    private val pipelineManager: PipelineManager,
    private val policyEngine: PolicyEngine,
) :
    AtlsController {
    override fun Route.registerRoutes() {
        authenticate {
            route("/pipeline") {
                post {
                    // This should then map to the appropriate assignee in the policy and should be checked by the
                    // policy evaluator
                    val principal = call.principal<UserIdPrincipal>()
                    val req = call.receive<PipelineCreateRequest>()

                    // TODO: Push this code down to some other class (ContractManager?)
                    // TODO: Make this responsive to sending the same contract again with different content but the same ID, so that the contract in the DB gets updated
                    val contract =
                        contractStore.getContract(req.contractId)

                    if (contract == null) {
                        LOG.atDebug {
                            message = "The requested contract could not be found in the contract store"
                            payload = mapOf("contract" to req.contractId, "target" to req.target, "action" to req.action)
                        }
                        call.respond(HttpStatusCode.NotFound)
                        return@post
                    }

                    val policyResult = policyEngine.evaluatePolicy(contract, URI(req.action ?: DEFAULT_ACTION), URI(req.target))
                    policyResult.map { eval ->
                        val pipeline = eval.containerPipeline
                        if (pipeline == null) {
                            LOG.atDebug {
                                message = "The contract evaluation yielded no container pipeline"
                                payload = mapOf("contract" to contract.uid, "target" to req.target, "action" to req.action)
                            }
                            call.respond(HttpStatusCode.BadRequest, "No matching container pipeline definition found")
                            return@map
                        }
                        call.respond(
                            PipelineCreateResponse(
                                pipeline.name,
                                pipeline.getContainerInfo(0)!!.ports,
                            ),
                        )
                    }.recover { err ->
                        LOG.atDebug {
                            message = "Error during policy evaluation"
                            cause = err
                            payload = mapOf("contract" to contract.uid, "action" to req.action, "target" to req.target)
                        }
                        when (err) {
                            is PolicyViolationException -> call.respond(HttpStatusCode.Forbidden, err.message ?: "Unknown Error")
                            else -> call.respond(HttpStatusCode.InternalServerError, err.message ?: "Unknown Error")
                        }
                    }
                }

                get("{id}") {
                    val principal = call.principal<UserIdPrincipal>()
                    val id = call.parameters["id"] ?: throw IllegalStateException("ID not found")

                    val status = pipelineManager.getPipelineStatus(id)
                    // Currently this only checks if the pipeline exists
                    call.respond(if (status) HttpStatusCode.OK else HttpStatusCode.NotFound)
                }

                delete("{id}") {
                    // TODO: Check if the caller is allowed to do this once we have authentication set up
                    val principal = call.principal<UserIdPrincipal>()
                    val id = call.parameters["id"] ?: throw IllegalStateException("ID not found")

                    val found = pipelineManager.deletePipeline(id)
                    call.respond(if (found) HttpStatusCode.OK else HttpStatusCode.NotFound)
                }
            }
        }
    }

    companion object {
        // The port that is exposed on the first container
        private const val DEFAULT_ACTION = OdrlIdentifiers.USE
        private const val DEFAULT_PORT = "8080/tcp"
        private val LOG = KotlinLogging.logger {}
    }
}
