/*
 * Copyright 2025-2026 Fraunhofer AISEC
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
package de.fraunhofer.aisec.glue.controllers.internal

import com.fasterxml.jackson.databind.JsonNode
import de.fraunhofer.aisec.glue.DataStore
import de.fraunhofer.aisec.glue.controllers.Controller
import de.fraunhofer.aisec.glue.negotiation.NegotiationManager
import de.fraunhofer.aisec.glue.negotiation.toOffer
import de.fraunhofer.aisec.glue.pipeline.PipelineManager
import de.fraunhofer.aisec.glue.pipeline.RemotePipelineManager
import de.fraunhofer.aisec.glue.policy.PolicyEngine
import de.fraunhofer.iosb.odrl.OdrlIdentifiers
import de.fraunhofer.iosb.odrl.OdrlParty
import de.fraunhofer.iosb.odrl.glue.GlueValueProcessor
import de.fraunhofer.iosb.odrl.parser.OdrlParser
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.http.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import java.net.URI

/**
 * This controller represents the interface that is exposed to the individual containers.
 */
class ContainerInterfaceController(
    private val negotiationManager: NegotiationManager,
    private val pipelineManager: PipelineManager,
    private val dataStore: DataStore,
    private val policyEngine: PolicyEngine,
    private val httpClient: HttpClient,
    private val odrlParser: OdrlParser,
    private val remotePipelineManager: RemotePipelineManager,
    atlsEnabled: Boolean,
) : Controller {
    private val remotePeerProtocol = if (atlsEnabled) "https" else "http"

    override fun Route.registerRoutes() {
        // Just IP-Address seems like it *could* be enough here (containers cannot really fake that as they cannot
        // That said, we could easily enough just inject a token for the container to authenticate with.
        route("/coordinator") {
            // Add a value for a pre-defined policy parameter
            post("/policy-parameter") {
                val caller = call.request.local.remoteAddress
                val parameters = call.receive<Map<String, JsonNode>>()
                val pipeline =
                    pipelineManager.getPipelineByIp(caller)
                        ?: throw NotFoundException("No associated pipeline could be found for caller with IP $caller")

                LOG.atDebug {
                    message = "Updating parameters for pipeline"
                    payload =
                        buildMap {
                            put("origin", caller)
                            put("oldParams", pipeline.placeholderValues)
                            put("newParams", parameters)
                            put("pipeline", pipeline.name)
                        }
                }

                pipeline.placeholderValues.putAll(parameters)
                call.respond(HttpStatusCode.Created)
            }

            // Negotiate a follow-up contract with the next step in the policy and start a pipeline
            // This function call is idempotent, meaning that the same call from the same container will not cause a
            // second contract to be negotiated.
            // Unless otherwise specified, the followup pipeline is started immediately.
            post("/negotiate") {
                val destination = call.queryParameters.getOrFail("destination")
                val startPipeline = call.queryParameters["startPipeline"]?.toBoolean() ?: true

                val callingAddress = call.request.local.remoteAddress
                val callingPipeline =
                    pipelineManager.getPipelineByIp(callingAddress) ?: run {
                        // We log at warn level here since this could indicate a misconfigured firewall
                        LOG.atWarn {
                            message = "Received negotiation request from source address not belonging to any pipeline"
                            payload =
                                mapOf(
                                    "origin" to callingAddress,
                                    "destination" to destination,
                                    "startPipeline" to startPipeline,
                                )
                        }
                        call.respond(HttpStatusCode.Forbidden)
                        return@post
                    }

                LOG.atDebug {
                    message = "Negotiating follow-up contract on behalf of container pipeline"
                    payload =
                        mapOf(
                            "origin" to callingAddress,
                            "destination" to destination,
                            "startPipeline" to startPipeline,
                            "callingPipeline" to callingPipeline.name,
                        )
                }

                // For now, we only allow the last container in the chain to start follow-up negotiations.
                //  capability
                if (!callingPipeline
                        .getContainerInfo()
                        .last()
                        .ipAddresses
                        .contains(callingAddress)
                ) {
                    LOG.atDebug {
                        message = "Rejecting request as the calling container is not last in the pipeline"
                        payload =
                            mapOf(
                                "origin" to callingAddress,
                                "destination" to destination,
                                "startPipeline" to startPipeline,
                                "callingPipeline" to callingPipeline.name,
                            )
                    }
                    call.respond(HttpStatusCode.Forbidden)
                    return@post
                }

                // Now we're done with verifying parameters and we can start with the actual negotiation

                // We default to port 8444 if the caller specifies no port
                val destinationHost =
                    if (destination.contains(":")) {
                        destination
                    } else {
                        "$destination:8444"
                    }

                // Check if we already have a follow-up contract that matches this target
                val contractCandidates =
                    callingPipeline.contracts(destinationHost).filter { c ->
                        c.permission.any { it.target?.uid?.toString() == callingPipeline.targetId }
                    }
                val followingContract =
                    if (contractCandidates.isNotEmpty()) {
                        contractCandidates.first().also { c ->
                            LOG.atDebug {
                                message = "Follow-up contract for the next step of the pipeline is already negotiated"
                                payload =
                                    mapOf(
                                        "origin" to callingAddress,
                                        "destination" to destination,
                                        "startPipeline" to startPipeline,
                                        "callingPipeline" to callingPipeline.name,
                                        "followingContract" to c.uid,
                                    )
                            }
                        }
                    } else {
                        val contract = dataStore.getContract(callingPipeline.contractId)
                        //  stopped?
                        assert(contract != null) {
                            "Contract for existing pipeline does not exist, which should not be possible"
                        }

                        val evaluation =
                            policyEngine
                                .evaluatePolicy(
                                    contract!!,
                                    URI(OdrlIdentifiers.DISTRIBUTE),
                                    URI(callingPipeline.targetId),
                                ).getOrElse { e ->
                                    LOG.atDebug {
                                        message = "Policy evaluation for next policy failed"
                                        cause = e
                                        payload =
                                            mapOf(
                                                "origin" to callingAddress,
                                                "destination" to destination,
                                                "startPipeline" to startPipeline,
                                                "callingPipeline" to callingPipeline.name,
                                            )
                                    }
                                    // Let's just blame the caller here ;)
                                    throw BadRequestException("Policy evaluation failed")
                                }

                        val nextPolicy =
                            GlueValueProcessor(odrlParser.mapper).resolvePlaceholders(
                                evaluation.nextPolicyTemplate
                                    ?: throw BadRequestException("The contract does not define an applicable follow-up policy"),
                            ) {
                                callingPipeline.placeholderValues[it]
                                    ?: throw NotFoundException("No value for placeholder $it")
                            }

                        val convertedNextPolicy =
                            odrlParser
                                .convertToOdrlPolicy(nextPolicy.json)
                                .toOffer(odrlParser, OdrlParty(URI("todo:name-this-connector")))

                        val negotiation =
                            negotiationManager.startNegotiation(
                                "$remotePeerProtocol://$destinationHost",
                                convertedNextPolicy,
                            )

                        LOG.atDebug {
                            message = "Negotiating contract with peer connector"
                            payload =
                                mapOf(
                                    "origin" to callingAddress,
                                    "destination" to destination,
                                    "startPipeline" to startPipeline,
                                    "callingPipeline" to callingPipeline.name,
                                    "offer" to convertedNextPolicy,
                                )
                        }

                        val negotiationResult = negotiation.awaitCompletion()
                        callingPipeline.addContract(destinationHost, negotiationResult)
                        negotiationResult
                    }

                LOG.atDebug {
                    message = "Obtained follow-up contract for use with next connector"
                    payload =
                        mapOf(
                            "origin" to callingAddress,
                            "destination" to destination,
                            "startPipeline" to startPipeline,
                            "callingPipeline" to callingPipeline.name,
                            "contract" to followingContract,
                        )
                }

                if (startPipeline) {
                    val remotePipeline =
                        remotePipelineManager.getOrCreate(
                            Url("$remotePeerProtocol://$destinationHost"),
                            followingContract.uid.toString(),
                            callingPipeline.targetId,
                        )
                    callingPipeline.addRemotePipeline(remotePipeline)

                    call.respond(remotePipeline.ports.map { p -> mapOf("endpoint" to p.hostAddress, "protocol" to p.protocol.uppercase()) })
                }
            }
        }
    }

    companion object {
        val LOG = KotlinLogging.logger {}
    }
}
