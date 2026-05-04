package de.fraunhofer.aisec.glue.controllers.api

import de.fraunhofer.aisec.glue.controllers.Controller
import de.fraunhofer.aisec.glue.pipeline.ContainerPipelineInfo
import de.fraunhofer.aisec.glue.pipeline.PipelineManager
import de.fraunhofer.aisec.glue.serialization.InlineId
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.util.getOrFail

class PipelineApiController(private val pipelineManager: PipelineManager) : Controller {
    override fun Route.registerRoutes() {
        route(ApiConstants.API_BASE_PATH) {
            route("/pipelines") {
                get {
                    val pipelines =
                        pipelineManager.getPipelines().map { (_, v) ->
                            ContainerPipelineInfo.from(v)
                        }
                    call.respond(pipelines)
                }

                get("{id}") {
                    val id = call.parameters.getOrFail<String>("id")
                    val pipeline = pipelineManager.getPipelineById(id)

                    if (pipeline != null) {
                        call.respond(ContainerPipelineInfo.from(pipeline))
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }

                delete("{id}") {
                    val id = call.parameters.getOrFail<String>("id")
                    val ok = pipelineManager.deletePipeline(id)

                    call.respond(if (ok) HttpStatusCode.OK else HttpStatusCode.NotFound)
                }
            }
        }
    }
}
