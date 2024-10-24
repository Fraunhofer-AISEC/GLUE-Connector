package de.fraunhofer.aisec.glue.controllers

import de.fraunhofer.aisec.glue.ContainerManager
import de.fraunhofer.aisec.glue.ContractStore
import de.fraunhofer.aisec.glue.models.pipeline.PipelineCreateRequest
import de.fraunhofer.aisec.glue.models.pipeline.PipelineCreateResponse
import fhg.glue.odrl.Context
import fhg.glue.odrl.GlueOdrlIdentifiers
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

/**
 * This controller provides an interface for other connectors to manage container pipelines associated with their
 * policies.
 */
class PipelineController(private val contractStore: ContractStore, private val containerManager: ContainerManager) :
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

                    withContext(coroutineContext) {
                        contract.evaluate(Context(req.action ?: "TODO: Default action?", req.target)) { action ->
                            when (action.uid) {
                                GlueOdrlIdentifiers.DEPLOY_PIPELINE -> {
                                    // Since the action callback is not marked with suspend, we have to resort to this
                                    launch {
                                        val (name, pipeline) =
                                            containerManager.createPipelineFromAction(
                                                action,
                                                publishPorts =
                                                listOf(
                                                    DEFAULT_PORT,
                                                ),
                                            )

                                        // Find the exposed port for the first container
                                        val containerDetails = pipeline.containers[0].inspect()
                                        pipelineCreateChannel.trySend(
                                            PipelineCreateResponse(
                                                name,
                                                containerDetails.networkSettings?.ports?.get(DEFAULT_PORT)
                                                    ?.get(0)?.hostPort?.toInt()
                                                    ?: throw RuntimeException("Error getting port")
                                            )
                                        )
                                    }
                                    true
                                }
                                // We don't know how to handle this action...
                                else -> {
                                    LOG.warn("Action ${action.uid} is not handled yet, ignoring for now...")
                                    true
                                }
                            }
                        }
                    }

                    // TODO: Make sure that a pipeline was actually created

                    // Now the container pipeline is set up, return the pipeline ID and port to the caller
                    call.respond(pipelineCreateChannel.tryReceive().getOrThrow())
                }

                get("{id}") {
                    val principal = call.principal<UserIdPrincipal>()
                    val id = call.parameters["id"]!!

                    val status = containerManager.getPipelineStatus(id)
                    // Currently this only checks if the pipeline exists
                    call.respond(if (status) HttpStatusCode.OK else HttpStatusCode.NotFound)
                }

                delete("{id}") {
                    // TODO: Check if the caller is allowed to do this once we have authentication set up
                    val principal = call.principal<UserIdPrincipal>()
                    val id = call.parameters["id"]!!

                    val found = containerManager.deletePipeline(id)
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
