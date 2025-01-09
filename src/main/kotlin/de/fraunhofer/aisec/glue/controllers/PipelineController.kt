package de.fraunhofer.aisec.glue.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import de.fraunhofer.aisec.glue.ContractStore
import de.fraunhofer.aisec.glue.models.pipeline.PipelineCreateRequest
import de.fraunhofer.aisec.glue.models.pipeline.PipelineCreateResponse
import de.fraunhofer.aisec.glue.negotiation.upcast
import de.fraunhofer.aisec.glue.pipeline.PipelineManager
import de.fraunhofer.iosb.odrl.OdrlConstraint
import de.fraunhofer.iosb.odrl.OdrlIdentifiers
import de.fraunhofer.iosb.odrl.evaluator.Context
import de.fraunhofer.iosb.odrl.evaluator.OdrlPolicyEvaluator
import de.fraunhofer.iosb.odrl.evaluator.Result
import de.fraunhofer.iosb.odrl.glue.GlueContainerPipeline
import de.fraunhofer.iosb.odrl.glue.GlueIdentifiers
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.channels.Channel
import org.slf4j.LoggerFactory

/**
 * This controller provides an interface for other connectors to manage container pipelines associated with their
 * policies.
 */
class PipelineController(private val objectMapper: ObjectMapper, private val contractStore: ContractStore, private val pipelineManager: PipelineManager) :
    Controller {
    override fun Route.registerRoutes() {
        authenticate {
            route("/pipeline") {
                post {
                    // This should then map to the appropriate assignee in the policy and should be checked by the
                    // policy evaluator
                    val principal = call.principal<UserIdPrincipal>()
                    val req = call.receive<PipelineCreateRequest>()

                    // TODO: Push this code down to some other class (ContractManager?)
                    val contract =
                        if (req.contractId != null) {
                            contractStore.getContract(req.contractId)
                        } else {
                            val contracts = contractStore.getContract(req.target, req.action)
                            when (contracts.size) {
                                0 -> null
                                1 -> contracts.first()
                                else -> {
                                    call.respond(HttpStatusCode.BadRequest)
                                    return@post
                                }
                            }
                        }

                    if (contract == null) {
                        call.respond(HttpStatusCode.NotFound)
                        return@post
                    }

                    // Note: We have to initialize this here
                    val pipelineCreateChannel = Channel<PipelineCreateResponse>(1)

                    OdrlPolicyEvaluator().evaluate(
                        Context(req.action ?: "TODO: Default action?", req.target),
                        contract.upcast(objectMapper),
                        actionCallback = { action ->
                            when (action.uid.toString()) {
                                GlueIdentifiers.DEPLOY_PIPELINE -> {
                                    val pipelineDefs =
                                        action.refinement.filter { r ->
                                            if (r is OdrlConstraint) {
                                                r.leftOperand.uid.toString() == GlueIdentifiers.HAS_CONTAINER_PIPELINE &&
                                                    r.operator.uid.toString() == OdrlIdentifiers.EQ
                                            } else {
                                                false
                                            }
                                        }.map { it as OdrlConstraint }

                                    if (pipelineDefs.isEmpty()) {
                                        throw Exception("A pipeline deploy action does not contain a pipeline definition")
                                    }
                                    // TODO: Is there a case where this could be fine (i.e. if further constraints exist)?
                                    if (pipelineDefs.size > 1) {
                                        throw Exception("A pipeline deploy action contains multiple pipeline definitions")
                                    }

                                    val pipelineDef =
                                        pipelineDefs[0].rightOperand.firstOrNull() as? GlueContainerPipeline
                                            ?: throw Exception(
                                                "Unexpected right operand type for container pipeline constraint",
                                            )

                                    val pipeline = pipelineManager.createPipeline(pipelineDef)

                                    // TODO: Wait until the containers are up/healthy using docker API
                                    // If not, the container pipeline might not report the port correctly

                                    // Find the exposed port for the first container
                                    val containerDetails = pipeline.getContainerInfo(0)!!
                                    pipelineCreateChannel.trySend(
                                        PipelineCreateResponse(
                                            pipeline.name,
                                            containerDetails.ports[0].hostPort.toInt(),
                                        ),
                                    )
                                    Result(true)
                                }
                                // We don't know how to handle this action...
                                else -> {
                                    LOG.warn("Action ${action.uid} is not handled yet, ignoring for now...")
                                    Result(true)
                                }
                            }
                        },
                    )

                    // TODO: Make sure that a pipeline was actually created

                    // Now the container pipeline is set up, return the pipeline ID and port to the caller
                    call.respond(pipelineCreateChannel.tryReceive().getOrThrow())
                }

                get("{id}") {
                    val principal = call.principal<UserIdPrincipal>()
                    val id = call.parameters["id"]!!

                    val status = pipelineManager.getPipelineStatus(id)
                    // Currently this only checks if the pipeline exists
                    call.respond(if (status) HttpStatusCode.OK else HttpStatusCode.NotFound)
                }

                delete("{id}") {
                    // TODO: Check if the caller is allowed to do this once we have authentication set up
                    val principal = call.principal<UserIdPrincipal>()
                    val id = call.parameters["id"]!!

                    val found = pipelineManager.deletePipeline(id)
                    call.respond(if (found) HttpStatusCode.OK else HttpStatusCode.NotFound)
                }
            }
        }
    }

    companion object {
        // The port that is exposed on the first container
        private const val DEFAULT_PORT = "8080/tcp"
        private val LOG = LoggerFactory.getLogger(PipelineController::class.java)
    }
}
