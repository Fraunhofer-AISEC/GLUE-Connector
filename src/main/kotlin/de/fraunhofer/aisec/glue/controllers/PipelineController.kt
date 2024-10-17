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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

class PipelineController(private val contractStore: ContractStore, private val containerManager: ContainerManager) : Controller {
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
                            // TODO: Return proper error to user on null
                            contractStore.getContract(req.contractId)!!
                        } else {
                            // TODO: Return error if size != 1
                            contractStore.getContract(req.target, req.action).first()
                        }

                    // Note: We have to initialize this here
                    var pipelineName = ""
                    var pipelinePort = 0

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
                                        pipelineName = name

                                        // Find the exposed port for the first container
                                        val containerDetails = pipeline.containers[0].inspect()
                                        // TODO: Clean this up
                                        pipelinePort = containerDetails.networkSettings!!.ports!![DEFAULT_PORT]!![0].hostPort!!.toInt()
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
                    call.respond(PipelineCreateResponse(pipelineName, pipelinePort))
                }

                get("{id}") {
                    // TODO: Return current status of the pipeline
                }

                delete("{id}") {
                    // TODO: Check if the caller is allowed to do this
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
