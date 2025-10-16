package de.fraunhofer.aisec.glue

import com.fasterxml.jackson.databind.ObjectMapper
import de.fraunhofer.aisec.glue.config.ConfigManager
import de.fraunhofer.aisec.glue.controllers.Controller
import de.fraunhofer.aisec.glue.controllers.NegotiationController
import de.fraunhofer.aisec.glue.controllers.PipelineController
import de.fraunhofer.aisec.glue.controllers.api.NegotiationApiController
import de.fraunhofer.aisec.glue.controllers.api.RemotePipelineApiController
import de.fraunhofer.aisec.glue.controllers.internal.ContainerInterfaceController
import de.fraunhofer.aisec.glue.negotiation.builtin.BuiltinNegotiationManager
import de.fraunhofer.aisec.glue.negotiation.builtin.DummyNegotiator
import de.fraunhofer.aisec.glue.pipeline.PipelineManager
import de.fraunhofer.aisec.glue.pipeline.RemotePipelineManager
import de.fraunhofer.aisec.glue.plugins.configureAuthentication
import de.fraunhofer.aisec.glue.policy.PolicyEngine
import de.fraunhofer.iosb.odrl.parser.OdrlParser
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.http.*
import io.ktor.serialization.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri
import io.ktor.server.routing.*
import org.kodein.di.*
import java.util.logging.Filter
import java.util.logging.Level
import java.util.logging.Logger
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

fun main(args: Array<String>) {
    val filteredLoggerPrefixes = listOf("sun.rmi", "javax.management")
    val filteredMethods =
        hashSetOf(
            Pair("java.io.ObjectInputFilter\$Config", "traceFilter"),
            Pair("java.io.ObjectInputStream", "filterCheck"),
        )
    Logger.getLogger("").apply {
        level = Level.ALL
        handlers.forEach { handler ->
            handler.level = Level.ALL
            // Filter background noise
            handler.filter =
                Filter { record ->
                    filteredLoggerPrefixes.none { record.loggerName.startsWith(it) } &&
                        Pair(record.sourceClassName, record.sourceMethodName) !in filteredMethods
                }
        }
    }
    EngineMain.main(args)
}

val defaultDependencies by DI.Module {
    bind<HttpClient>() with
        singleton {
            HttpClient(CIO) {
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
    bind<OdrlParser>() with singleton { OdrlParser() }
    bind<ObjectMapper>() with singleton { instance<OdrlParser>().mapper }
    bind<ContentConverter>() with singleton { JacksonConverter(instance()) }
    bind<RemotePipelineManager>() with singleton { RemotePipelineManager(instance(), instance()) }
    bind<PipelineManager>() with singleton { PipelineManager(instance(), instance()) }
    bind<PolicyEngine>() with singleton { PolicyEngine(instance()) }
    bind<ContractStore>() with singleton { ContractStore(instance()) }
    bind<BuiltinNegotiationManager>() with
        singleton {
            BuiltinNegotiationManager(instance(), instance(), DummyNegotiator(), instance(), instance(), instance())
        }
    bind<NegotiationController>() with singleton { NegotiationController(instance()) }
    bind<PipelineController>() with singleton { PipelineController(instance(), instance(), instance()) }
    bind<NegotiationApiController>() with singleton { NegotiationApiController(instance()) }
    bind<RemotePipelineApiController>() with singleton { RemotePipelineApiController(instance(), instance(), instance()) }
    bind<ContainerInterfaceController>() with
        singleton {
            ContainerInterfaceController(instance(), instance(), instance(), instance(), instance(), instance(), instance())
        }
}

fun Application.module() =
    moduleWithDeps(
        DI.direct {
            import(defaultDependencies)
            import(
                DI.Module("config") {
                    // Since environment.config is part of the Application, we can only create these bindings here
                    bind<ConfigManager>() with singleton { ConfigManager(environment.config) }
                },
            )
        },
    )

fun Application.moduleWithDeps(dependencies: DirectDI) {
    configureAuthentication()
    install(CORS) {
        anyHost()
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.ContentType)
    }

    install(ServerContentNegotiation) {
        register(ContentType.Application.Json, dependencies.instance())
    }

    install(CallLogging) {
        level = org.slf4j.event.Level.INFO
        format { call ->
            val status = call.response.status()
            val httpMethod = call.request.httpMethod.value
            val uri = call.request.uri
            val userAgent = call.request.headers["User-Agent"]
            "$httpMethod $uri -> $status (User-Agent: $userAgent)"
        }
    }

    routing {
        dependencies.allInstances<Controller>().forEach { it.apply { registerRoutes() } }
    }
}
