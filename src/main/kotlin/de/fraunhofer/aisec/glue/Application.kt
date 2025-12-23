package de.fraunhofer.aisec.glue

import com.fasterxml.jackson.core.JsonFactoryBuilder
import com.fasterxml.jackson.core.StreamReadFeature
import com.fasterxml.jackson.databind.ObjectMapper
import de.fraunhofer.aisec.glue.atls.AtlsManager
import de.fraunhofer.aisec.glue.atls.NettyAtlsHandshakeHandler
import de.fraunhofer.aisec.glue.config.ConfigManager
import de.fraunhofer.aisec.glue.controllers.AtlsController
import de.fraunhofer.aisec.glue.controllers.Controller
import de.fraunhofer.aisec.glue.controllers.NegotiationController
import de.fraunhofer.aisec.glue.controllers.PipelineController
import de.fraunhofer.aisec.glue.controllers.api.NegotiationApiController
import de.fraunhofer.aisec.glue.controllers.api.RemotePipelineApiController
import de.fraunhofer.aisec.glue.controllers.internal.ContainerInterfaceController
import de.fraunhofer.aisec.glue.docker.DockerClient
import de.fraunhofer.aisec.glue.negotiation.builtin.BuiltinNegotiationManager
import de.fraunhofer.aisec.glue.negotiation.builtin.DummyNegotiator
import de.fraunhofer.aisec.glue.pipeline.Backend
import de.fraunhofer.aisec.glue.pipeline.PipelineManager
import de.fraunhofer.aisec.glue.pipeline.RemotePipelineManager
import de.fraunhofer.aisec.glue.pipeline.atls.AtlsParameter
import de.fraunhofer.aisec.glue.pipeline.docker.DockerBackend
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
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.netty.handler.ssl.ClientAuth
import io.netty.handler.ssl.JdkSslContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.kodein.di.*
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.logging.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

typealias NettyServer = EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>

fun main(args: Array<String>) {
    val filteredLoggerPrefixes = listOf("sun.rmi", "javax.management")
    val filteredMethods =
        hashSetOf(
            Pair("java.io.ObjectInputFilter\$Config", "traceFilter"),
            Pair("java.io.ObjectInputStream", "filterCheck"),
            Pair("jdk.internal.event.EventHelper", "logX509CertificateEvent"),
        )
    Logger.getLogger("").apply {
        level = Level.ALL
        handlers.forEach { handler ->
            handler.level = Level.ALL
            // Configure the timestamp format with microseconds in ISO-8601 format with local timezone
            handler.formatter =
                object : SimpleFormatter() {
                    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX")

                    override fun format(record: LogRecord): String {
                        val timestamp = ZonedDateTime.now(ZoneId.systemDefault()).format(dateFormatter)
                        val message = formatMessage(record)
                        val throwable =
                            if (record.thrown != null) {
                                val sw = java.io.StringWriter()
                                record.thrown.printStackTrace(java.io.PrintWriter(sw))
                                "\n$sw"
                            } else {
                                ""
                            }
                        return "$timestamp [${record.level}] ${record.loggerName} - $message$throwable\n"
                    }
                }
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
                    // Required as we need to wait for a remote container to fetch the atls-proxy image in the start
                    // pipeline request
                    socketTimeoutMillis = 180000
                }
                engine {
                    config {
                        val atlsManager = instance<AtlsManager>()
                        sslSocketFactory(atlsManager.socketFactory, atlsManager.trustManager)
                    }
                }
            }
        }
    bind<OdrlParser>() with
        singleton {
            OdrlParser(
                ObjectMapper(JsonFactoryBuilder().enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION).build()).apply {
                    registerSubtypes(AtlsParameter::class.java)
                },
            )
        }
    bind<ObjectMapper>() with singleton { instance<OdrlParser>().mapper }
    bind<DockerClient>() with singleton { DockerClient.fromEnv() }
    bind<ContentConverter>() with singleton { JacksonConverter(instance()) }
    bind<RemotePipelineManager>() with singleton { RemotePipelineManager(instance(), instance()) }
    bind<Backend>() with singleton { DockerBackend(instance(), instance(), instance()) }
    bind<PipelineManager>() with singleton { PipelineManager(instance(), instance(), instance()) }
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
    bind<CoroutineDispatcher>() with singleton { Dispatchers.IO }
    bind<AtlsManager>() with singleton { AtlsManager(instance(), instance(), instance()) }
    bind<NettyServer>() with
        singleton {
            val atlsManager = instance<AtlsManager>()
            embeddedServer(
                Netty,
                environment =
                    applicationEnvironment {
                        config = MapApplicationConfig()
                    },
                module = {
                    configureAuthentication()
                    install(ServerContentNegotiation) {
                        register(ContentType.Application.Json, instance())
                    }
                    routing {
                        allInstances<AtlsController>().forEach { it.apply { registerRoutes() } }
                    }
                },
                configure = {
                    connector {
                        host = "0.0.0.0"
                        port = 8444
                    }
                    channelPipelineConfig = {
                        // Build SSL context with CMC backing
                        val handler =
                            JdkSslContext(
                                atlsManager.sslContext,
                                false,
                                null,
                                io.netty.handler.ssl.IdentityCipherSuiteFilter.INSTANCE,
                                null,
                                ClientAuth.REQUIRE,
                                null,
                                false,
                            ).newHandler(channel().alloc())

                        // codec is the first handler guaranteed to be registered by the ktor HTTP 1.1 pipeline.
                        // We don't make use of the TLS implementation supplied by ktor as it expects a more traditional
                        // keystore/truststore setup than we use.
                        addBefore("codec", "ssl", handler)
                        addAfter(
                            "ssl",
                            "atls-handshake",
                            NettyAtlsHandshakeHandler(
                                instance(),
                                atlsManager.cmcClient,
                                instance(),
                            ),
                        )
                    }
                },
            ).start(wait = false)
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

    // Start the ATLS server by resolving it
    dependencies.instance<NettyServer>()
}
