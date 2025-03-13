package de.fraunhofer.aisec.glue.controllers.internal

import com.fasterxml.jackson.databind.JsonNode
import de.fraunhofer.aisec.glue.ContractStore
import de.fraunhofer.aisec.glue.Utils
import de.fraunhofer.aisec.glue.controllers.Controller
import de.fraunhofer.aisec.glue.models.pipeline.PipelineCreateRequest
import de.fraunhofer.aisec.glue.models.pipeline.PipelineCreateResponse
import de.fraunhofer.aisec.glue.negotiation.NegotiationManager
import de.fraunhofer.aisec.glue.negotiation.toOffer
import de.fraunhofer.aisec.glue.pipeline.PipelineManager
import de.fraunhofer.aisec.glue.policy.PolicyEngine
import de.fraunhofer.iosb.odrl.OdrlIdentifiers
import de.fraunhofer.iosb.odrl.OdrlParty
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
import java.net.InetAddress
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
) : Controller {
    override fun Route.registerRoutes() {
        // TOOD: Insert some container-side authentication
        // Just IP-Address seems like it *could* be enough here (containers cannot really fake that as they cannot
        // send raw packets (TODO: is there any other way to fake the source address that work in containers?).
        // That said, we could easily enough just inject a token for the container to authenticate with.
        route("/coordinator") {
            // Add a value for a pre-defined policy parameter
            post("/policy-parameter") {
                val caller = call.request.local.remoteAddress
                // TODO(long-term): Add some validation to detect if the container is allowed to supply that parameter
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

            post("/proxy") {
                val logParams = mutableMapOf<String, Any>()

                val destination = call.queryParameters.getOrFail("destination")
                val port = call.queryParameters["port"] // TODO: Change the pipeline create response to allow more ports
                // TODO(long-term?): We also need to check for target here since a policy could have more than one

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
                        // TODO: Is not found really the right exception here?
                        pipeline.placeholderValues[it] ?: throw NotFoundException("No value for placeholder $it")
                    }

                // Split the destination address into usable parts
                val destinationURI = URI(destination)
                val destinationHost = destinationURI.host
                val destinationPath = destinationURI.path

                // Negotiate the policy with the next connector

                val convertedNextPolicy =
                    OdrlParser().convertToOdrlPolicy(nextPolicy.json)
                        .toOffer(OdrlParty(URI("todo:name-this-connector")))

                // HACK: This will not work in production since we will likely not be reachable by hostname alone
                // This only works in docker compose or similar environments.
                val originUrl = InetAddress.getLocalHost().hostAddress
                // TODO: Check if we already have an agreement with the destination node
                val negotiation =
                    negotiationManager.startNegotiation(
                        Url("http://$destinationHost:8080/negotiations"),
                        Url("http://$originUrl:8080/negotiations"),
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

                // TODO: Store the actual response in the remotely managed pipelines that is also managed by the uapi
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

                val appUrl = "http://$destinationHost:${remotePipeline.pipelinePort}$destinationPath"
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
