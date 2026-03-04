package de.fraunhofer.aisec.glue.controllers.api

import de.fraunhofer.aisec.glue.DataStore
import de.fraunhofer.aisec.glue.controllers.Controller
import de.fraunhofer.aisec.glue.models.api.RemotePipelineCreationRequest
import de.fraunhofer.aisec.glue.models.api.RemotePipelineStatusResponse
import de.fraunhofer.aisec.glue.pipeline.RemotePipelineManager
import de.fraunhofer.iosb.odrl.OdrlIdentifiers
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * This controller provides an overview of the pipelines created by this connector on other connectors.
 */
class RemotePipelineApiController(
    @Suppress("unused") private val dataStore: DataStore,
    private val remotePipelineManager: RemotePipelineManager,
) : Controller {
    override fun Route.registerRoutes() {
        route(ApiConstants.API_BASE_PATH) {
            route("/remote-pipelines") {
                get {
                    val pipelines = remotePipelineManager.getAll()
                    call.respond(pipelines)
                }

                post {
                    val req = call.receive<RemotePipelineCreationRequest>()
                    val peerUrl = Url(if (req.peer.contains("://")) req.peer else "http://${req.peer}")

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
                    call.respond(if (ok) HttpStatusCode.OK else HttpStatusCode.NotFound)
                }
            }
        }
    }

    companion object {
        val LOG = KotlinLogging.logger {}
    }
}
