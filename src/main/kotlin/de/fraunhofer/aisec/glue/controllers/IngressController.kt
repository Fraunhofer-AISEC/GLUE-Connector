package de.fraunhofer.aisec.glue.controllers

import de.fraunhofer.aisec.glue.ContainerManager
import de.fraunhofer.aisec.glue.ContractManager
import de.fraunhofer.aisec.glue.PDP
import de.fraunhofer.aisec.glue.models.Contract
import de.fraunhofer.aisec.glue.proxy.Proxy
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

class IngressController(
    private val contractManager: ContractManager,
    private val pdp: PDP,
    private val containerManager: ContainerManager,
    private val proxy: Proxy
) : Controller {
    override fun Route.registerRoutes() {
        authenticate("ingress-auth") {
            // TODO: It is a little bit annoying that we currently have to always give the whole path here
            //       Kodein would theoretically offer ktor integration to allow injecting a subcontroller for routes I
            //       think, but that would bind us to the DI framework
            //       Alternatively, we could define all controllers with factories, which allow us to create controllers
            //       with the necessary parameters passed as arguments on the fly.
            route("/contract/{contractId}/call/{path...}") {
                handle {
                    // TODO: Mainly fetch an actual contract object here instead
                    val contractId = call.parameters["contractId"]!!
                    val contract = contractManager.get(contractId)
                    val callerIdentity = call.principal<UserIdPrincipal>()?.name ?: "Test"
                    val reqMethod = call.request.httpMethod
                    val path = call.parameters.getAll("path")!!.joinToString("/")

                    if (contract == null) {
                        call.respond(HttpStatusCode.NotFound)
                        return@handle
                    }

                    if (!pdp.checkHttpCall(contract, callerIdentity, reqMethod.value, path)) {
                        call.respond(HttpStatusCode.Unauthorized)
                        return@handle
                    }

                    // TODO: Handle null, which indicates that the contract does not specify any mapping for this path
                    val containerUrl = resolveContainerUrl(contract, path)!!

                    // TODO: Inject proxy as well
                    val resp = proxy.delegate(call.request, containerUrl)
                    call.respond(resp)
                }
            }
        }
    }

    private suspend fun resolveContainerUrl(
        contract: Contract,
        path: String,
    ): String? {
        val pipeline = containerManager.pipelineForContract(contract) ?: return null

        // TODO: For now, we always map to the first container on port 8080
        //       Eventually, we may want the caller to be able to specify its own port and the destination container by
        //       name.
        //       We may also need to allow the path to change.
        val containerInfo = pipeline.containers.getOrNull(0)?.inspect() ?: return null
        // We can use any IP address of the container, since we are in all networks.
        // TODO: Make sure each container in the pipeline has at least 1 IP address
        //       This is currently not the case if the pipeline contains only one container
        val containerNetwork = containerInfo.networkSettings?.networks?.values?.firstOrNull() ?: return null
        val containerIp = containerNetwork.ipAddress ?: return null

        return "http://$containerIp:8080/$path"
    }
}
