package de.fraunhofer.aisec.glue.plugins

import de.fraunhofer.aisec.glue.ContainerPipeline
import de.fraunhofer.aisec.glue.Ingress
import de.fraunhofer.aisec.glue.PDP
import de.fraunhofer.aisec.glue.createContainerCreateReq
import de.fraunhofer.aisec.glue.models.Pipeline
import de.fraunhofer.aisec.glue.proxy.Proxy
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import kotlin.collections.set

fun Application.configureRouting() {
    val pipelines by inject<MutableMap<String, ContainerPipeline>>()
    val pdp by inject<PDP>()

    routing {
        // TODO: As I understand it, this route is to be called with the already negotiated contract, right?
        //       So we should probably add some authentication here as well
        route("contract") {
            post {
                // TODO: Contracts from EDC should be submitted to here for parsing.
                //       Return some form of contract id for the client to access the deployment with later
            }
        }

        route("contract/{contractId}") {
            // TODO: Authentication requirement should probably be applied to all routes eventually
            authenticate("ingress-auth") {
                route("call/{path...}") {
                    handle {
                        val contract = call.parameters["contractId"]!!
                        val callerIdentity = call.principal<UserIdPrincipal>()!!.name
                        val reqMethod = call.request.httpMethod
                        val path = call.parameters.getAll("path")!!.joinToString("/")

                        if (!pdp.checkHttpCall(contract, callerIdentity, reqMethod.value, path)) {
                            call.respond(HttpStatusCode.Unauthorized)
                            return@handle
                        }

                        // TODO: Just for testing at this point.
                        //       We should just have one ingress per contract that should be constructed with the actual
                        //       pipeline at creation time.
                        val ingress = Ingress(contract, pipelines[contract]!!)
                        val containerUrl = ingress.resolveContainerURL(path)!!

                        val resp = Proxy.proxy(call.request, containerUrl)
                        call.respond(resp)
                    }
                }
            }
        }

        // Example for Testing/Demo purposes
        route("pipeline") {
            post {
                val pipeline = call.receive<Pipeline>()
                val containers =
                    pipeline.containers.map {
                        Pair(it.name, createContainerCreateReq(image = it.image, cmd = it.cmd))
                    }
                pipelines[pipeline.id] = ContainerPipeline.create(containers).apply { start() }
                call.respondText("Successfully created pipeline \"${pipeline.id}\"", status = HttpStatusCode.Created)
            }
            delete("{id?}") {
                val id =
                    call.parameters["id"] ?: return@delete call.respondText(
                        "Missing id",
                        status = HttpStatusCode.BadRequest,
                    )
                val pipeline =
                    pipelines[id] ?: return@delete call.respondText(
                        "Pipeline not found",
                        status = HttpStatusCode.NotFound,
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
