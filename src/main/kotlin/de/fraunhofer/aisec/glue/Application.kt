package de.fraunhofer.aisec.glue

import com.fasterxml.jackson.databind.ObjectMapper
import de.fraunhofer.aisec.glue.controllers.Controller
import de.fraunhofer.aisec.glue.controllers.NegotiationController
import de.fraunhofer.aisec.glue.controllers.PipelineController
import de.fraunhofer.aisec.glue.controllers.api.NegotiationApiController
import de.fraunhofer.aisec.glue.controllers.api.RemotePipelineApiController
import de.fraunhofer.aisec.glue.negotiation.DummyNegotiator
import de.fraunhofer.aisec.glue.negotiation.NegotiationManager
import de.fraunhofer.aisec.glue.pipeline.PipelineManager
import de.fraunhofer.aisec.glue.plugins.configureAuthentication
import de.fraunhofer.aisec.glue.policy.PolicyEngine
import de.fraunhofer.iosb.odrl.parser.OdrlParser
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import org.kodein.di.*
import java.util.logging.LogManager
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

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
                install(ClientContentNegotiation) {
                    register(ContentType.Application.Json, JacksonConverter(instance()))
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
    bind<PipelineManager>() with singleton { PipelineManager(instance()) }
    bind<PolicyEngine>() with singleton { PolicyEngine(instance()) }
    bind<ContractStore>() with singleton { ContractStore(instance()) }
    bind<NegotiationManager>() with singleton { NegotiationManager(instance(), instance(), DummyNegotiator(), instance()) }
    bind<NegotiationController>() with singleton { NegotiationController(instance()) }
    bind<PipelineController>() with singleton { PipelineController(instance(), instance(), instance()) }
    bind<NegotiationApiController>() with singleton { NegotiationApiController(instance()) }
    bind<RemotePipelineApiController>() with singleton { RemotePipelineApiController(instance(), instance()) }
}

fun Application.module() = module(DI { import(defaultDependencies) })

fun Application.module(dependencies: DI) {
    configureAuthentication()

    install(ServerContentNegotiation) {
        val mapper by dependencies.instance<ObjectMapper>()
        register(ContentType.Application.Json, JacksonConverter(mapper))
    }

    routing {
        val controllers by dependencies.allInstances<Controller>()
        controllers.forEach { c -> c.apply { registerRoutes() } }
    }
}
