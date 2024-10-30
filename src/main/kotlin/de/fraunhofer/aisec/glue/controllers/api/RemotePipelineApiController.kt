package de.fraunhofer.aisec.glue.controllers.api

import de.fraunhofer.aisec.glue.ContractStore
import de.fraunhofer.aisec.glue.Utils.hexEncode
import de.fraunhofer.aisec.glue.controllers.Controller
import de.fraunhofer.aisec.glue.models.api.RemotePipelineCreationRequest
import de.fraunhofer.aisec.glue.models.api.RemotePipelineStatusResponse
import de.fraunhofer.aisec.glue.models.pipeline.PipelineCreateResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

/**
 * This controller provides an overview of the pipelines created by this connector on other connectors.
 */
class RemotePipelineApiController(
    private val httpClient: HttpClient,
    private val contractStore: ContractStore
) : Controller {
    // TODO: Extract this state from the controller
    //  With that, also add proper typing
    // Currently, the triple is (peer address, remote pipeline id, pipeline port)
    private val managedPipelines = mutableMapOf<String, Triple<String, String, Int>>()
    private val pipelinesMutex = Mutex()

    override fun Route.registerRoutes() {
        // TODO: Authorization
        route("/api/remote-pipelines") {
            get {
                val pipelines = managedPipelines.map { (id, pipeline) ->
                    val (peer, remoteName, remotePort) = pipeline
                    RemotePipelineStatusResponse(peer, id, PipelineCreateResponse(remoteName, remotePort))
                }
                call.respond(pipelines)
            }

            post {
                val req = call.receive<RemotePipelineCreationRequest>()
                // TODO: Again, I'd prefer to have some kind of internal ID for the peer connector
                val peerUrl = run {
                    val address = if (req.peer.contains("://")) req.peer else "http://${req.peer}"
                    URLBuilder(address).appendPathSegments("pipeline").build()
                }

                // TODO: Should we check if the contract actually exists?
                //  Technically, it would be possible to have a different connector do the negotiation, but then why
                //  use this proxy function here at all...

                val resp = httpClient.post(peerUrl) {
                    header("Content-Type", ContentType.Application.Json)
                    setBody(req.request)
                }

                if (!resp.status.isSuccess()) {
                    call.respond(
                        HttpStatusCode.BadGateway,
                        "Could not create the requested pipeline on the remote connector: ${resp.bodyAsText()}"
                    )
                    return@post
                }

                val remotePipeline = try {
                     resp.body<PipelineCreateResponse>()
                } catch (e: NoTransformationFoundException) {
                    call.respond(
                        HttpStatusCode.BadGateway,
                        "Could not deserialize the response from the remote connector"
                    )
                    return@post
                }

                val localId = pipelinesMutex.withLock {
                    val name = if (remotePipeline.pipelineName !in managedPipelines) remotePipeline.pipelineName else createPipelineId()
                    managedPipelines[name] = Triple(req.peer, remotePipeline.pipelineName, remotePipeline.pipelinePort)
                    name
                }

                call.respond(HttpStatusCode.Created, RemotePipelineStatusResponse(req.peer, localId, remotePipeline))
            }

            delete("{id}") {
                val id = call.parameters["id"]!!
                // TODO: To maintain consistency, we can technically only remove the local pipeline entry once the
                //  remote pipeline is actually deleted.
                //  If we don't do this, the remote pipeline may stay around if the later delete request to the peer
                //  fails.
                //  For now, this is ok, especially since the locking situation would get more complicated otherwise.
                val pipeline = pipelinesMutex.withLock {
                    managedPipelines.remove(id)
                }

                if (pipeline == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@delete
                }

                val (peer, remoteName, _) = pipeline
                val peerUrl = run {
                    val address = if (peer.contains("://")) peer else "http://${peer}"
                    URLBuilder(address).appendPathSegments("pipeline", remoteName).build()
                }

                val resp = httpClient.delete(peerUrl)
                if (!resp.status.isSuccess()) {
                    // TODO: This is the critical point where we should not delete the local object unless this succeeds
                    call.respond(HttpStatusCode.BadGateway, "Error deleting on the remote connector")
                }
            }
        }
    }

    private fun createPipelineId(): String =
        generateSequence { Random.Default.nextBytes(8).hexEncode() }.first { n -> !managedPipelines.contains(n) }
}