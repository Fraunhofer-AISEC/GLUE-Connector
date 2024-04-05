package de.fraunhofer.aisec.glue.plugins

import de.fraunhofer.aisec.glue.ContainerPipeline
import de.fraunhofer.aisec.glue.createContainerCreateReq
import de.fraunhofer.aisec.glue.models.Pipeline
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

val pipelines = mutableMapOf<String, ContainerPipeline>()

fun Application.configureRouting() {
    routing {
        route("contract") {
            post {
            }
        }
        // Example for Testing/Demo purposes
        route("pipeline") {
            post {
                val pipeline = call.receive<Pipeline>()
                val containers = pipeline.containers.map {
                    Pair(it.name, createContainerCreateReq(image = it.image, cmd = it.cmd))
                }
                pipelines[pipeline.id] = ContainerPipeline.create(containers).apply { start() }
                call.respondText("Successfully created pipeline \"${pipeline.id}\"", status = HttpStatusCode.Created)
            }
            delete("{id?}") {
                val id = call.parameters["id"] ?: return@delete call.respondText(
                    "Missing id",
                    status = HttpStatusCode.BadRequest
                )
                val pipeline = pipelines[id] ?: return@delete call.respondText(
                    "Pipeline not found",
                    status = HttpStatusCode.NotFound
                )
                pipeline.apply {
                    stop()
                    remove()
                }
                call.respondText("Successfully stopped and deleted pipeline \"${id}\"", status = HttpStatusCode.OK)
            }
        }
    }
}