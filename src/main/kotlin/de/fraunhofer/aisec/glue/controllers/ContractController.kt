package de.fraunhofer.aisec.glue.controllers

import de.fraunhofer.aisec.glue.ContainerManager
import de.fraunhofer.aisec.glue.ContainerPipeline
import de.fraunhofer.aisec.glue.ContractManager
import de.fraunhofer.aisec.glue.models.Contract
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

class ContractController(
    private val contractManager: ContractManager,
    private val containerManager: ContainerManager,
) : Controller {
    private val containerPipelines = mutableMapOf<String, ContainerPipeline>()

    override fun Route.registerRoutes() {
        post("/contract") {
            val contract = call.receive<Contract>()
            containerManager.pipelineForContract(contract)?.start()

            val contractId = contractManager.register(contract)
            call.respondText(contractId)
        }

        delete("/contract/{id}") {
            val contractId = call.parameters["id"]!!
            val contract = contractManager.get(contractId)

            if (contract == null) {
                call.respond(HttpStatusCode.NotFound)
                return@delete
            }

            containerManager.pipelineForContract(contract)?.apply {
                stop()
                remove()
            }
            contractManager.deregister(contractId)
        }
    }
}
