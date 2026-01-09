package de.fraunhofer.aisec.glue.controllers.api

import de.fraunhofer.aisec.glue.controllers.Controller
import de.fraunhofer.aisec.glue.models.api.NegotiationCreationRequest
import de.fraunhofer.aisec.glue.models.api.NegotiationInfoResponse
import de.fraunhofer.aisec.glue.negotiation.NegotiationManager
import de.fraunhofer.aisec.glue.pipeline.PipelineManager
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
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
        // TODO: Authorization
        route(ApiConstants.API_BASE_PATH) {
            route("/negotiations") {
                // List all negotiations that are currently ongoing on the connector
                get {
                    val negotiations =
                        negotiationManager.getNegotiations()
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
                    // TODO: The peer field from the request should probably be some sort of internal ID from which
                    //  the url and protocol version is derived.
                    //  For not, it is just the URL with some simple parsing.
                    val peerUrl =
                        run {
                            val address = if (req.peer.contains("://")) req.peer else "http://${req.peer}"
                            val builder = URLBuilder(address)

                            if (builder.pathSegments.isEmpty()) {
                                builder.path("negotiations")
                            }

                            builder.buildString()
                        }

                    val negotiationFuture = negotiationManager.startNegotiation(peerUrl, req.contractRequest)

                    if (negotiationFuture.isCompleted) {
                        try {
                            negotiationFuture.await()
                            // This is supposed to fail
                            // If not, we have some really weird scheduling going on, but so be it
                        } catch (e: Exception) {
                            LOG.atDebug {
                                message = "Contract negotiation completed exceptionally"
                                cause = e
                                payload = mapOf("peerConnector" to req.peer, "contractRequest" to req.contractRequest.uid)
                            }
                            // Note: This could also be an internal server error, but we will assume
                            // that it's the user's fault
                            call.respond(HttpStatusCode.BadRequest, "$e")
                            return@post
                        }
                    }

                    // If we have a local pipeline, spawn it on success
                    if (req.localPipeline != null) {
                        CoroutineScope(Dispatchers.Default).launch {
                            // The error is handled in the negotiation subsystem, we just need to know to
                            val contract = runCatching { negotiationFuture.await() }.getOrElse { return@launch }

                            // Spawn a local pipeline with a terminating ATLS proxy that will spawn the remote pipeline.
                            // TODO: Note that the handling of the ATLS proxy is a bit dodgy at the moment, which will be
                            //  addressed in the next MR.
                            val pipeline =
                                pipelineManager.createPipeline(
                                    req.localPipeline,
                                    "Local Pipeline for ${contract.uid}",
                                    contract.permission[0].target!!.uid.toString(),
                                )
                            LOG.atInfo {
                                message = "Created local pipeline for contract negotiation"
                                payload = mapOf("peer" to req.peer, "contract" to contract.uid, "pipeline" to pipeline.name)
                            }

                            // TODO: This is the hacky part. We add the contract as an already negotiated follow-up contract for the
                            //  ATLS proxy to not trigger another negotiation.
                            //  This could break in a number of ways, e.g. if the peer property does not match or if the target
                            //  differs.
                            val peerUrl = URI(req.peer)
                            pipeline.addContract("${peerUrl.host}:${peerUrl.port}", contract)
                            // pipeline.start()
                        }
                    }

                    call.respond(HttpStatusCode.Created)
                }

                delete {
                    // TODO: Delete the negotiation object
                    // TODO: How to identify? (providerPid, consumerPid)? some internal ID?
                    LOG.error { "Contract negotiation can currently not be cancelled" }
                }
            }
        }
    }

    companion object {
        val LOG = KotlinLogging.logger {}
    }
}
