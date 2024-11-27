package de.fraunhofer.aisec.glue

import com.apicatalog.jsonld.JsonLdOptions
import com.fasterxml.jackson.databind.ObjectMapper
import de.fraunhofer.aisec.glue.controllers.Controller
import de.fraunhofer.aisec.glue.controllers.NegotiationController
import de.fraunhofer.aisec.glue.controllers.PipelineController
import de.fraunhofer.aisec.glue.controllers.api.NegotiationApiController
import de.fraunhofer.aisec.glue.controllers.api.RemotePipelineApiController
import de.fraunhofer.aisec.glue.jsonld.JsonLDModule
import de.fraunhofer.aisec.glue.negotiation.DummyNegotiator
import de.fraunhofer.aisec.glue.negotiation.NegotiationManager
import de.fraunhofer.aisec.glue.pipeline.PipelineManager
import de.fraunhofer.aisec.glue.plugins.configureAuthentication
import de.fraunhofer.aisec.glue.plugins.configureSerialization
import de.fraunhofer.iosb.odrl.parser.OdrlParser
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.netty.EngineMain
import io.ktor.server.routing.*
import org.kodein.di.*
import org.kodein.type.jvmType
import java.util.logging.LogManager

fun main(args: Array<String>) {
    LogManager.getLogManager().getLogger("").let {
        it.level = java.util.logging.Level.ALL
        it.handlers.forEach { h -> h.level = java.util.logging.Level.ALL }
    }
    EngineMain.main(args)
}

val defaultDependencies by DI.Module {
    bind<HttpClient>() with
        singleton {
            HttpClient(OkHttp) {
                install(ContentNegotiation) {
                    jackson {
                        registerModules(JsonLDModule(JsonLdOptions()))
                    }
                }
                install(Auth) {
                    basic {
                        credentials {
                            BasicAuthCredentials("user", "password")
                        }
                    }
                }
            }
        }
    bind<ObjectMapper>() with singleton { OdrlParser().mapper }
    bind<ContainerManager>() with singleton { ContainerManager(instance()) }
    bind<PipelineManager>() with singleton { PipelineManager(instance()) }
    bind<NegotiationManager>() with singleton { NegotiationManager(instance(), instance(), DummyNegotiator()) }
    bind<ContractStore>() with singleton { ContractStore(instance(), instance()) }
    bind<NegotiationController>() with singleton { NegotiationController(instance()) }
    bind<PipelineController>() with singleton { PipelineController(instance(), instance()) }
    bind<NegotiationApiController>() with singleton { NegotiationApiController(instance()) }
    bind<RemotePipelineApiController>() with singleton { RemotePipelineApiController(instance(), instance()) }
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
