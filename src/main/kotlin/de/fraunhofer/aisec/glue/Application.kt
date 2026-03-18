package de.fraunhofer.aisec.glue

import com.fasterxml.jackson.core.JsonFactoryBuilder
import com.fasterxml.jackson.core.StreamReadFeature
import com.fasterxml.jackson.databind.ObjectMapper
import de.fraunhofer.aisec.glue.atls.AtlsManager
import de.fraunhofer.aisec.glue.atls.NettyAtlsHandshakeHandler
import de.fraunhofer.aisec.glue.config.ConfigManager
import de.fraunhofer.aisec.glue.config.ConnectorConfig
import de.fraunhofer.aisec.glue.controllers.*
import de.fraunhofer.aisec.glue.controllers.api.NegotiationApiController
import de.fraunhofer.aisec.glue.controllers.api.PipelineApiController
import de.fraunhofer.aisec.glue.controllers.api.RemotePipelineApiController
import de.fraunhofer.aisec.glue.controllers.internal.ContainerInterfaceController
import de.fraunhofer.aisec.glue.docker.DockerClient
import de.fraunhofer.aisec.glue.logging.LogbackObjectMapperInjector
import de.fraunhofer.aisec.glue.negotiation.NegotiationManager
import de.fraunhofer.aisec.glue.negotiation.builtin.BuiltinNegotiationManager
import de.fraunhofer.aisec.glue.negotiation.builtin.DummyNegotiator
import de.fraunhofer.aisec.glue.negotiation.edc.EdcQuirks.makeOutgoingEdcPatcher
import de.fraunhofer.aisec.glue.pipeline.Backend
import de.fraunhofer.aisec.glue.pipeline.PipelineManager
import de.fraunhofer.aisec.glue.pipeline.RemotePipelineManager
import de.fraunhofer.aisec.glue.pipeline.atls.AtlsConfig
import de.fraunhofer.aisec.glue.pipeline.atls.AtlsParameter
import de.fraunhofer.aisec.glue.pipeline.docker.DockerBackend
import de.fraunhofer.aisec.glue.plugins.ClientEdcCompatTransformation
import de.fraunhofer.aisec.glue.plugins.configureAuthentication
import de.fraunhofer.aisec.glue.policy.PolicyEngine
import de.fraunhofer.iosb.odrl.parser.OdrlParser
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.netty.handler.ssl.ClientAuth
import io.netty.handler.ssl.JdkSslContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.kodein.di.*
import java.net.ConnectException
import kotlin.math.pow
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

typealias NettyServer = EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>

fun main(args: Array<String>) {
    EngineMain.main(args)
}

val LOG = KotlinLogging.logger {}

val defaultDependencies by DI.Module {
    bind<HttpClient>() with
        singleton {
            val atlsConfig = instance<ConfigManager>().getConfig(::AtlsConfig)
            HttpClient(OkHttp) {
                if (instance<ConnectorConfig>().edcMode == ConnectorConfig.EdcMode.COMPAT) {
                    val objectMapper = instance<ObjectMapper>()
                    install(ClientEdcCompatTransformation) {
                        transformRequest = makeOutgoingEdcPatcher(objectMapper, "client")
                    }
                }
                install(DefaultRequest) {
                    header(HttpHeaders.UserAgent, Constants.TC_USER_AGENT)
                }
                install(ClientContentNegotiation) {
                    register(ContentType.Application.Json, instance())
                }
                install(Auth) {
                    basic {
                        credentials {
                            val (username, password) = Utils.getCredentials()
                            BasicAuthCredentials(username, password)
                        }
                    }
                }
                install(HttpTimeout) {
                    // Required as we need to wait for a remote container to fetch the atls-proxy image in the start
                    // pipeline request
                    socketTimeoutMillis = 180000
                }
                install(HttpRequestRetry) {
                    maxRetries = 10
                    retryOnExceptionIf { _, cause ->
                        cause is ConnectException
                    }
                    exponentialDelay(base = 1.5, maxDelayMs = 30000)
                }
                if (atlsConfig.enabled) {
                    engine {
                        config {
                            val atlsManager = instance<AtlsManager>()
                            sslSocketFactory(
                                atlsManager.socketFactory,
                                atlsManager.trustManager,
                            )
                        }
                    }
                }
            }.apply {
                // Monitor retry events for logging
                monitor.subscribe(HttpRequestRetryEvent) { event ->
                    val delayBase = 1.5
                    val nextDelay =
                        (delayBase * 1000 * delayBase.pow(event.retryCount.toDouble() - 1)).toLong()
                            .coerceAtMost(30000)
                    LOG.atWarn {
                        message = "Retrying HTTP request"
                        payload =
                            mapOf(
                                "url" to event.request.url.toString(),
                                "retryCount" to event.retryCount,
                                "nextDelayMs" to nextDelay,
                            )
                    }
                }
            }
        }
    bind<OdrlParser>() with
        singleton {
            OdrlParser(
                mapper =
                    ObjectMapper(
                        JsonFactoryBuilder().enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
                            .build(),
                    ).apply {
                        registerSubtypes(AtlsParameter::class.java)
                    },
                compactSerialization = true,
            )
        }
    bind<ObjectMapper>() with singleton { instance<OdrlParser>().mapper }
    bind<DockerClient>() with singleton { DockerClient.fromEnv() }
    bind<ContentConverter>() with singleton { JacksonConverter(instance()) }
    bind<RemotePipelineManager>() with singleton { RemotePipelineManager(instance(), instance()) }
    bind<Backend>() with singleton { DockerBackend(instance(), instance(), instance()) }
    bind<PipelineManager>() with singleton { PipelineManager(instance(), instance(), instance()) }
    bind<PolicyEngine>() with singleton { PolicyEngine(instance()) }
    bind<DataStore>() with singleton { DataStore(instance()) }
    bind<ConnectorConfig>() with singleton { instance<ConfigManager>().getConfig(::ConnectorConfig) }
    bind<NegotiationManager>() with
        singleton {
            BuiltinNegotiationManager(
                instance(),
                instance(),
                DummyNegotiator(),
                instance(),
                instance(),
                instance(),
            )
        }
    bind<NegotiationController>() with
        singleton {
            when (instance<ConnectorConfig>().edcMode) {
                else -> BuiltinNegotiationController(instance(), instance(), instance())
            }
        }
    bind<PipelineController>() with
        singleton {
            PipelineController(
                instance(),
                instance(),
                instance(),
            )
        }
    bind<NegotiationApiController>() with
        singleton {
            NegotiationApiController(
                instance(),
                instance(),
            )
        }
    bind<PipelineApiController>() with singleton { PipelineApiController(instance()) }
    bind<RemotePipelineApiController>() with
        singleton {
            RemotePipelineApiController(
                instance(),
                instance(),
            )
        }
    bind<AtlsConfig>() with singleton { instance<ConfigManager>().getConfig(::AtlsConfig) }
    bind<ContainerInterfaceController>() with
        singleton {
            ContainerInterfaceController(
                instance(),
                instance(),
                instance(),
                instance(),
                instance(),
                instance(),
                instance(),
                instance<AtlsConfig>().enabled,
            )
        }
    bind<CoroutineDispatcher>() with singleton { Dispatchers.IO }
    bind<AtlsManager>() with singleton { AtlsManager(instance(), instance(), instance()) }
    bind<NettyServer>() with
        singleton {
            val atlsConfig = instance<AtlsConfig>()
            embeddedServer(
                Netty,
                environment =
                    applicationEnvironment {
                        config = MapApplicationConfig()
                    },
                module = {
                    configureAuthentication()
                    install(DefaultHeaders) {
                        header(HttpHeaders.Server, Constants.TC_USER_AGENT)
                    }
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
                        if (atlsConfig.enabled) {
                            val atlsManager = instance<AtlsManager>()
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
    LogbackObjectMapperInjector.register(dependencies.instance())

    configureAuthentication()
    install(DefaultHeaders) {
        header(HttpHeaders.Server, Constants.TC_USER_AGENT)
    }
    install(CORS) {
        anyHost()
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.ContentType)
    }
    install(ServerContentNegotiation) {
        register(ContentType.Application.Json, dependencies.instance())
    }
    install(CallLogging) {
        level = org.slf4j.event.Level.TRACE
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
