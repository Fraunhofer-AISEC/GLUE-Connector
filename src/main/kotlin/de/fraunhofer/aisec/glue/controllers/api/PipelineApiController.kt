/*
 * Copyright 2026 Fraunhofer AISEC
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

class PipelineApiController(
    private val pipelineManager: PipelineManager,
) : Controller {
    override fun Route.registerRoutes() {
        route(ApiConstants.API_BASE_PATH) {
            route("/pipelines") {
                get {
                    val pipelines =
                        pipelineManager.getPipelines().map { (k, v) ->
                            InlineId(k, ContainerPipelineInfo.from(v))
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
