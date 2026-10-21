/*
 * Copyright 2024-2026 Fraunhofer AISEC
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
package de.fraunhofer.aisec.glue.controllers

import de.fraunhofer.aisec.glue.DataStore
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
    private val dataStore: DataStore,
    private val pipelineManager: PipelineManager,
    private val policyEngine: PolicyEngine,
) : AtlsController {
    override fun Route.registerRoutes() {
        route("/pipeline") {
            post {
                // This should then map to the appropriate assignee in the policy and should be checked by the
                // policy evaluator
                val principal = call.principal<UserIdPrincipal>()
                val req = call.receive<PipelineCreateRequest>()

                val contract =
                    dataStore.getContract(req.contractId)

                // Responds with `NotFound` if contract missing
                if (contract == null) {
                    LOG.atDebug {
                        message = "The requested contract could not be found in the contract store"
                        payload = mapOf("contract" to req.contractId, "target" to req.target, "action" to req.action)
                    }
                    call.respond(HttpStatusCode.NotFound)
                    return@post
                }

                val policyResult = policyEngine.evaluatePolicy(contract, URI(req.action ?: DEFAULT_ACTION), URI(req.target))
                policyResult
                    .map { eval ->
                        val pipeline = eval.containerPipeline
                        // Responds with `BadRequest` if no pipeline found
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
                val principal = call.principal<UserIdPrincipal>()
                val id = call.parameters["id"] ?: throw IllegalStateException("ID not found")

                val found = pipelineManager.deletePipeline(id)
                call.respond(if (found) HttpStatusCode.OK else HttpStatusCode.NotFound)
            }
        }
    }

    companion object {
        // The port that is exposed on the first container
        private const val DEFAULT_ACTION = OdrlIdentifiers.USE
        private val LOG = KotlinLogging.logger {}
    }
}
