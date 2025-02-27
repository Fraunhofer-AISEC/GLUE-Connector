package de.fraunhofer.aisec.glue

import com.fasterxml.jackson.databind.ObjectMapper
import de.fraunhofer.aisec.glue.controllers.Controller
import de.fraunhofer.aisec.glue.controllers.NegotiationController
import de.fraunhofer.aisec.glue.controllers.PipelineController
import de.fraunhofer.aisec.glue.controllers.api.NegotiationApiController
import de.fraunhofer.aisec.glue.controllers.api.RemotePipelineApiController
import de.fraunhofer.aisec.glue.controllers.internal.ContainerInterfaceController
import de.fraunhofer.aisec.glue.negotiation.DummyNegotiator
import de.fraunhofer.aisec.glue.negotiation.NegotiationManager
import de.fraunhofer.aisec.glue.pipeline.PipelineManager
import de.fraunhofer.aisec.glue.plugins.configureAuthentication
import de.fraunhofer.aisec.glue.policy.PolicyEngine
import de.fraunhofer.iosb.odrl.parser.OdrlParser
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.http.*
import io.ktor.serialization.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import org.kodein.di.*
import java.util.logging.Filter
import java.util.logging.Level
import java.util.logging.Logger
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

fun main(args: Array<String>) {
    val filteredLoggerPrefixes = listOf("sun.rmi", "javax.management")
    val filteredMethods = hashSetOf(
        Pair("java.io.ObjectInputFilter\$Config", "traceFilter"),
        Pair("java.io.ObjectInputStream", "filterCheck"),
    )
    Logger.getLogger("").apply {
        level = Level.ALL
        handlers.forEach { handler ->
            handler.level = Level.ALL
            // Filter background noise
            handler.filter = Filter { record ->
                filteredLoggerPrefixes.none { record.loggerName.startsWith(it) }
                        && Pair(record.sourceClassName, record.sourceMethodName) !in filteredMethods
            }
        }
    }
    EngineMain.main(args)
}

val defaultDependencies by DI.Module {
    bind<HttpClient>() with
        singleton {
            HttpClient(OkHttp) {
                install(ClientContentNegotiation) {
                    register(ContentType.Application.Json, instance())
                }
                install(Auth) {
                    basic {
                        credentials {
                            val(username, password) = Utils.getCredentials()
                            BasicAuthCredentials(username, password)
                        }
                    }
                }
                install(HttpTimeout) {
                    socketTimeoutMillis = 30000
                }
            }
        }
    bind<ObjectMapper>() with singleton { OdrlParser().mapper }
    bind<ContentConverter>() with singleton { JacksonConverter(instance()) }
    bind<PipelineManager>() with singleton { PipelineManager(instance()) }
    bind<PolicyEngine>() with singleton { PolicyEngine(instance()) }
    bind<ContractStore>() with singleton { ContractStore(instance()) }
    bind<NegotiationManager>() with singleton { NegotiationManager(instance(), instance(), DummyNegotiator(), instance()) }
    bind<NegotiationController>() with singleton { NegotiationController(instance()) }
    bind<PipelineController>() with singleton { PipelineController(instance(), instance(), instance()) }
    bind<NegotiationApiController>() with singleton { NegotiationApiController(instance()) }
    bind<RemotePipelineApiController>() with singleton { RemotePipelineApiController(instance(), instance()) }
    bind<ContainerInterfaceController>() with singleton { ContainerInterfaceController(instance(), instance(), instance(), instance(), instance()) }
}

fun Application.module() = module(DI.direct { import(defaultDependencies) })

fun Application.module(dependencies: DirectDI) {
    configureAuthentication()

    install(ServerContentNegotiation) {
        register(ContentType.Application.Json, dependencies.instance())
    }

    routing {
        dependencies.allInstances<Controller>().forEach { it.apply { registerRoutes() } }
    }
}
