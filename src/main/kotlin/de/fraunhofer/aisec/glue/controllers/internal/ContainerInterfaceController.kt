package de.fraunhofer.aisec.glue.controllers.internal

import com.fasterxml.jackson.databind.JsonNode
import de.fraunhofer.aisec.glue.ContractStore
import de.fraunhofer.aisec.glue.Utils
import de.fraunhofer.aisec.glue.controllers.Controller
import de.fraunhofer.aisec.glue.models.pipeline.PipelineCreateRequest
import de.fraunhofer.aisec.glue.models.pipeline.PipelineCreateResponse
import de.fraunhofer.aisec.glue.negotiation.NegotiationManager
import de.fraunhofer.aisec.glue.negotiation.toOffer
import de.fraunhofer.aisec.glue.pipeline.ContainerPipeline
import de.fraunhofer.aisec.glue.pipeline.PipelineManager
import de.fraunhofer.aisec.glue.pipeline.RemotePipelineManager
import de.fraunhofer.aisec.glue.policy.PolicyEngine
import de.fraunhofer.iosb.odrl.OdrlIdentifiers
import de.fraunhofer.iosb.odrl.OdrlParty
import de.fraunhofer.iosb.odrl.OdrlPolicy
import de.fraunhofer.iosb.odrl.glue.GlueValueProcessor
import de.fraunhofer.iosb.odrl.parser.OdrlParser
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import io.ktor.utils.io.*
import java.net.URI

/**
 * This controller represents the interface that is exposed to the individual containers.
 */
class ContainerInterfaceController(
    private val negotiationManager: NegotiationManager,
    private val pipelineManager: PipelineManager,
    private val contractStore: ContractStore,
    private val policyEngine: PolicyEngine,
    private val httpClient: HttpClient,
    private val odrlParser: OdrlParser,
    private val remotePipelineManager: RemotePipelineManager,
) : Controller {
    val remotePipelines: MutableMap<Pair<ContainerPipeline, OdrlPolicy>, Pair<String, String>> = mutableMapOf()

    override fun Route.registerRoutes() {
        // TOOD: Insert some container-side authentication
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
                if (!callingPipeline.getContainerInfo().last().ipAddresses.contains(callingAddress)) {
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

                // Split the destination address into usable parts
                val destinationURI = URI(destination)
                val destinationHost = destinationURI.host

                // Check if we already have a follow-up contract that matches this target
                val contractCandidates =
                    callingPipeline.contracts(destinationHost).filter {
                            c ->
                        c.permission.any { p -> p.target?.uid?.toString() == callingPipeline.targetId }
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
                        val contract = contractStore.getContract(callingPipeline.contractId)
                        //  stopped?
                        assert(contract != null) {
                            "Contract for existing pipeline does not exist, which should not be possible"
                        }

                        val evaluation =
                            policyEngine.evaluatePolicy(
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
                            GlueValueProcessor().resolvePlaceholders(
                                evaluation.nextPolicyTemplate
                                    ?: throw BadRequestException("The contract does not define an applicable follow-up policy"),
                            ) {
                                callingPipeline.placeholderValues[it]
                                    ?: throw NotFoundException("No value for placeholder $it")
                            }

                        val convertedNextPolicy =
                            odrlParser.convertToOdrlPolicy(nextPolicy.json)
                                .toOffer(odrlParser, OdrlParty(URI("todo:name-this-connector")))

                        val negotiation =
                            negotiationManager.startNegotiation(
                                "http://$destinationHost:8080/negotiations",
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

                        val negotiationResult = negotiation.await()
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
                    /*// TODO: Add some form of remote pipeline registry to manage remote pipelines started with the API
                    //  and new policies
                    val remotePipeline = remotePipelines[callingPipeline to followingContract]
                    if (remotePipeline != null) {
                        val (remoteAddr, remoteId) = remotePipeline
                        LOG.atDebug {
                            message = "Remote pipeline already exists, checking liveness"
                            payload =
                                mapOf(
                                    "origin" to callingAddress,
                                    "destination" to destination,
                                    "callingPipeline" to callingPipeline.name,
                                    "remotePipeline" to remoteId,
                                )
                        }

                        val statusResponse = httpClient.get("http://$destinationHost:8080/pipeline/$remoteId")
                        if (statusResponse.status.isSuccess()) {
                            LOG.atDebug {
                                message = "Remote pipeline is up"
                                payload =
                                    mapOf(
                                        "origin" to callingAddress,
                                        "destination" to destination,
                                        "callingPipeline" to callingPipeline.name,
                                        "remotePipeline" to remoteId,
                                    )
                            }

                            // We return a list of endpoints here alongside their protocol ("TCP" and "UDP")
                            call.respond(listOf(mapOf("endpoint" to remoteAddr, "protocol" to "TCP")))
                            return@post
                        }

                        LOG.atDebug {
                            message = "Existing pipeline is down or invalid, recreating"
                            payload =
                                mapOf(
                                    "origin" to callingAddress,
                                    "destination" to destination,
                                    "callingPipeline" to callingPipeline.name,
                                    "remotePipeline" to remoteId,
                                )
                        }

                        remotePipelines.remove(callingPipeline to followingContract)
                    }

                    LOG.atDebug {
                        message = "Creating pipeline on next connector"
                        payload =
                            mapOf(
                                "origin" to callingAddress,
                                "destination" to destination,
                                "callingPipeline" to callingPipeline.name,
                            )
                    }

                    val createReq =
                        PipelineCreateRequest(
                            target = callingPipeline.targetId,
                            contractId = followingContract.uid.toString(),
                            action = OdrlIdentifiers.USE,
                        )

                    val createResp =
                        httpClient.post("http://$destinationHost:8080/pipeline") {
                            contentType(ContentType.Application.Json)
                            setBody(createReq)
                        }

                    if (!createResp.status.isSuccess()) {
                        val body = createResp.bodyAsText()
                        LOG.atDebug {
                            message = "Could not create remote pipeline"
                            payload =
                                mapOf(
                                    "origin" to callingAddress,
                                    "destination" to destination,
                                    "callingPipeline" to callingPipeline.name,
                                    "status" to createResp.status,
                                    "description" to body,
                                )
                        }
                        //  code for some reason.
                        call.respond(HttpStatusCode.BadGateway, body)
                        return@post
                    }

                    val createdPipeline = createResp.body<PipelineCreateResponse>()

                    LOG.atDebug {
                        message = "Created new pipeline on the remote host"
                        payload =
                            mapOf(
                                "origin" to callingAddress,
                                "destination" to destination,
                                "callingPipeline" to callingPipeline.name,
                                "remotePipeline" to createdPipeline.pipelineName,
                            )
                    }

                    //  does not seem to work otherwise (something about docker refusing to resolve using another docker
                    //  daemon's dns server...)
                    val remoteAddr = createdPipeline.ports[0].hostAddress

                    remotePipelines[callingPipeline to followingContract] = remoteAddr to createdPipeline.pipelineName

                    call.respond(listOf(mapOf("endpoint" to remoteAddr, "protocol" to "TCP")))
                     */

                    val remotePipeline =
                        remotePipelineManager.getOrCreate(
                            Url("http://$destinationHost:8080"),
                            followingContract.uid.toString(),
                            callingPipeline.targetId,
                        )

                    call.respond(remotePipeline.ports.map { p -> mapOf("endpoint" to p.hostAddress, "protocol" to p.protocol.uppercase()) })
                }
            }

            post("/proxy") {
                val logParams = mutableMapOf<String, Any>()

                val destination = call.queryParameters.getOrFail("destination")
                val port = call.queryParameters["port"] // TODO: Change the pipeline create response to allow more ports

                LOG.atDebug {
                    message = "Servicing proxy request from container"
                    payload =
                        logParams.apply {
                            put("origin", call.request.local.remoteAddress)
                            put("destination", destination)
                        }
                }

                // Obtain the next policy to use on the third container.

                val pipeline =
                    pipelineManager.getPipelineByIp(call.request.local.remoteAddress)
                        ?: throw NotFoundException("Pipeline not found")
                val contract =
                    contractStore.getContract(pipeline.contractId) ?: throw NotFoundException("Contract not found")

                LOG.atDebug {
                    message = "Obtained contract for pipeline"
                    payload =
                        logParams.apply {
                            put("pipeline", pipeline.name)
                            put("contract", contract.uid)
                            put("params", pipeline.placeholderValues)
                        }
                }

                val evaluationResult =
                    policyEngine.evaluatePolicy(contract, URI(OdrlIdentifiers.DISTRIBUTE), URI(pipeline.targetId))

                LOG.atDebug {
                    message = "Next policy evaluation ${if (evaluationResult.isSuccess) "successful" else "failed"}"
                    cause = evaluationResult.exceptionOrNull()
                    payload = logParams
                }

                val evaluation = evaluationResult.getOrElse { e -> throw BadRequestException("Policy evaluation failed", e) }

                val nextPolicy =
                    GlueValueProcessor().resolvePlaceholders(
                        evaluation.nextPolicyTemplate ?: throw BadRequestException("No follow-up policy found"),
                    ) {
                        pipeline.placeholderValues[it] ?: throw NotFoundException("No value for placeholder $it")
                    }

                // Split the destination address into usable parts
                val destinationURI = URI(destination)
                val destinationHost = destinationURI.host
                val destinationPath = destinationURI.path

                // Negotiate the policy with the next connector

                val convertedNextPolicy =
                    odrlParser.convertToOdrlPolicy(nextPolicy.json)
                        .toOffer(odrlParser, OdrlParty(URI("todo:name-this-connector")))

                val negotiation =
                    negotiationManager.startNegotiation(
                        "http://$destinationHost:8080/negotiations",
                        convertedNextPolicy,
                    )

                LOG.atDebug {
                    message = "Entering contract negotiation with destination connector"
                    payload = logParams // TODO: Obtain some more info from the negotiation somehow
                }

                val newPolicy = negotiation.await()

                // Spawn the pipeline on the next container
                LOG.atDebug {
                    message = "Creating pipeline on destination connector"
                    payload = logParams
                }

                val createReq =
                    PipelineCreateRequest(
                        target = pipeline.targetId,
                        action = OdrlIdentifiers.USE,
                        contractId = newPolicy.uid.toString(),
                    )

                // Wa may also want to add some sort of owner to it, so that the remote pipeline is stopped when this
                // one shuts down (Assuming that is what we want).
                val createResp =
                    httpClient.post("http://$destinationHost:8080/pipeline") {
                        header("Content-Type", ContentType.Application.Json)
                        setBody(createReq)
                        val (username, password) = Utils.getCredentials()
                        basicAuth(username, password)
                    }

                if (!createResp.status.isSuccess()) {
                    call.respond(HttpStatusCode.BadGateway)
                    return@post
                }

                val remotePipeline = createResp.body<PipelineCreateResponse>()

                LOG.atDebug {
                    message = "Created pipeline on destination connector"
                    payload = logParams.apply { put("remotePipeline", remotePipeline) }
                }

                // Proxy the actual request to the next connector

                val appUrl = "http://${remotePipeline.ports[0].hostAddress}$destinationPath"
                val appResponse =
                    httpClient.post(appUrl) {
                        setBody(call.request.receiveChannel())
                    }

                LOG.info { "Proxy request returned ${appResponse.status}" }

                call.respond(
                    object : OutgoingContent.WriteChannelContent() {
                        override suspend fun writeTo(channel: ByteWriteChannel) {
                            appResponse.bodyAsChannel().copyTo(channel)
                        }
                    },
                )
            }
        }
    }

    companion object {
        val LOG = KotlinLogging.logger {}
    }
}
