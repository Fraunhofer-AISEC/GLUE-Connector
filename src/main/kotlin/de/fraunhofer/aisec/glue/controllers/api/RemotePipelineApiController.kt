package de.fraunhofer.aisec.glue.controllers.api

import de.fraunhofer.aisec.glue.ContractStore
import de.fraunhofer.aisec.glue.controllers.Controller
import de.fraunhofer.aisec.glue.models.api.RemotePipelineCreationRequest
import de.fraunhofer.aisec.glue.models.api.RemotePipelineStatusResponse
import de.fraunhofer.aisec.glue.pipeline.RemotePipelineManager
import de.fraunhofer.iosb.odrl.OdrlIdentifiers
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * This controller provides an overview of the pipelines created by this connector on other connectors.
 */
class RemotePipelineApiController(
    private val httpClient: HttpClient,
    private val contractStore: ContractStore,
    private val remotePipelineManager: RemotePipelineManager,
) : Controller {
    override fun Route.registerRoutes() {
        // TODO: Authorization
        route("/api/remote-pipelines") {
            get {
                val pipelines = remotePipelineManager.getAll()
                call.respond(pipelines)
            }

            post {
                val req = call.receive<RemotePipelineCreationRequest>()
                // TODO: Again, I'd prefer to have some kind of internal ID for the peer connector
                val peerUrl = Url(if (req.peer.contains("://")) req.peer else "http://${req.peer}")

                // TODO: Should we check if the contract actually exists?
                //  Technically, it would be possible to have a different connector do the negotiation, but then why
                //  use this proxy function here at all...

                val remotePipeline =
                    remotePipelineManager.create(
                        peerUrl,
                        req.request.contractId,
                        req.request.target,
                        req.request.action ?: OdrlIdentifiers.USE,
                    )

                call.respond(
                    HttpStatusCode.Created,
                    RemotePipelineStatusResponse(
                        req.peer,
                        remotePipeline.id,
                        remotePipeline.remoteId,
                        remotePipeline.ports,
                    ),
                )
            }

            delete("{id}") {
                val id = call.parameters["id"] ?: throw IllegalStateException("ID not found")
                val ok = remotePipelineManager.delete(id)
                // TODO: Other errors
                call.respond(if (ok) HttpStatusCode.OK else HttpStatusCode.NotFound)
            }
        }
    }

    companion object {
        val LOG = KotlinLogging.logger {}
    }
}
