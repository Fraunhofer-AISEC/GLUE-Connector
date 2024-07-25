package de.fraunhofer.aisec.glue

import de.fraunhofer.aisec.glue.controllers.ContractController
import de.fraunhofer.aisec.glue.controllers.Controller
import de.fraunhofer.aisec.glue.controllers.IngressController
import de.fraunhofer.aisec.glue.controllers.NegotiationController
import de.fraunhofer.aisec.glue.plugins.configureAuthentication
import de.fraunhofer.aisec.glue.plugins.configureSerialization
import de.fraunhofer.aisec.glue.proxy.Proxy
import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.kodein.di.*
import org.kodein.type.jvmType
import java.util.logging.LogManager

fun main(args: Array<String>) {
    LogManager.getLogManager().getLogger("").let {
        it.level = java.util.logging.Level.ALL
        it.handlers.forEach { h -> h.level = java.util.logging.Level.ALL }
    }
    io.ktor.server.netty.EngineMain.main(args)
}

val defaultDependencies by DI.Module {
    bind<PDP>() with singleton { PDP() }
    bind<ContractManager>() with singleton { ContractManager() }
    bind<ContainerManager>() with singleton { ContainerManager(instance()) }
    bind<ContractController>() with singleton { ContractController(instance(), instance()) }
    bind<IngressController>() with singleton { IngressController(instance(), instance(), instance(), instance()) }
    bind<NegotiationController>() with singleton { NegotiationController() }
    bind<Proxy>() with singleton { Proxy() }
}

fun Application.module() = module(DI { import(defaultDependencies) })

fun Application.module(dependencies: DI) {
    configureAuthentication()
    // configureRouting(dependencies)
    configureSerialization()

    routing {
        // https://github.com/ktorio/ktor-samples/blob/c447878b213b6e612bdd835b250bd67fae24a336/di-kodein/src/main/kotlin/io/ktor/samples/kodein/KodeinAdvancedApplication.kt
        for (binding in dependencies.container.tree.bindings) {
            val bindingType = binding.key.type.jvmType as? Class<*>?
            if (bindingType != null && Controller::class.java.isAssignableFrom(bindingType)) {
                val controller by dependencies.Instance(binding.key.type)
                (controller as Controller).apply { registerRoutes() }
            }
        }
    }
}
