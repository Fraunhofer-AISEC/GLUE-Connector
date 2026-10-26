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
package de.fraunhofer.aisec.glue.controllers.api

import de.fraunhofer.aisec.glue.controllers.Controller
import de.fraunhofer.aisec.glue.models.api.NegotiationCreationRequest
import de.fraunhofer.aisec.glue.models.api.NegotiationInfoResponse
import de.fraunhofer.aisec.glue.negotiation.NegotiationManager
import de.fraunhofer.aisec.glue.pipeline.PipelineManager
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URI

/**
 * This controller allows a user to initiate and manage contract negotiation processes.
 */
class NegotiationApiController(
    private val negotiationManager: NegotiationManager,
    private val pipelineManager: PipelineManager,
) : Controller {
    override fun Route.registerRoutes() {
        route(ApiConstants.API_BASE_PATH) {
            route("/negotiations") {
                // List all negotiations that are currently ongoing on the connector
                get {
                    val negotiations =
                        negotiationManager
                            .getNegotiations()
                            .map { negotiation ->
                                NegotiationInfoResponse(
                                    id = negotiation.id,
                                    role = negotiation.role,
                                    peer = negotiation.peerIdentity ?: "unknown",
                                    state = negotiation.state,
                                )
                            }

                    call.respond(negotiations)
                }

                post {
                    val req = call.receive<NegotiationCreationRequest>()
                    val peerUrl =
                        if (req.peer.contains("://")) {
                            req.peer
                        } else {
                            "https://${req.peer}"
                        }

                    val negotiation = negotiationManager.startNegotiation(peerUrl, req.contractRequest)

                    // If we have a local pipeline, spawn it on success
                    if (req.localPipeline != null) {
                        CoroutineScope(Dispatchers.Default).launch {
                            // The error is handled in the negotiation subsystem, we just need to know to
                            val contract = runCatching { negotiation.awaitCompletion() }.getOrElse { return@launch }

                            // Spawn a local pipeline with a terminating ATLS proxy that will spawn the remote pipeline.
                            //  addressed in the next MR.
                            val pipeline =
                                pipelineManager.createPipeline(
                                    req.localPipeline,
                                    //  current connector.
                                    contract.uid.toString(),
                                    contract.permission[0]
                                        .target!!
                                        .uid
                                        .toString(),
                                )
                            LOG.atInfo {
                                message = "Created local pipeline for contract negotiation"
                                payload = mapOf("peer" to req.peer, "contract" to contract.uid, "pipeline" to pipeline.name)
                            }

                            val peerUrl = URI(peerUrl)
                            pipeline.addContract("${peerUrl.host}:${peerUrl.port}", contract)
                        }
                    }

                    call.respond(
                        HttpStatusCode.Created,
                        NegotiationInfoResponse(
                            id = negotiation.id,
                            role = negotiation.role,
                            peer = negotiation.peerIdentity ?: "unknown",
                            state = negotiation.state,
                        ),
                    )
                }

                get("{id}") {
                    val id = call.parameters.getOrFail<String>("id")
                    val negotiation = negotiationManager.getNegotiation(id) ?: throw NotFoundException()
                    call.respond(
                        NegotiationInfoResponse(
                            id = negotiation.id,
                            role = negotiation.role,
                            peer = negotiation.peerIdentity ?: "unknown",
                            state = negotiation.state,
                        ),
                    )
                }

                delete {
                    LOG.error { "Contract negotiation can currently not be cancelled" }
                }
            }
        }
    }

    companion object {
        val LOG = KotlinLogging.logger {}
    }
}
