package de.fraunhofer.aisec.glue.tools

import com.fasterxml.jackson.module.kotlin.readValue
import de.fraunhofer.aisec.glue.ContractStore
import de.fraunhofer.aisec.glue.config.ConfigManager
import de.fraunhofer.aisec.glue.config.ConnectorConfig
import de.fraunhofer.aisec.glue.controllers.BuiltinNegotiationController
import de.fraunhofer.aisec.glue.negotiation.builtin.BuiltinNegotiationManager
import de.fraunhofer.aisec.glue.negotiation.builtin.DummyNegotiator
import de.fraunhofer.aisec.glue.plugins.configureAuthentication
import de.fraunhofer.iosb.odrl.OdrlOffer
import de.fraunhofer.iosb.odrl.parser.OdrlParser
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.system.exitProcess
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

fun main(args: Array<String>) {
    val usage = "Usage: [-l localAddress] -c connectorAddress -p policyPath"
    val objectMapper = OdrlParser().mapper

    var localHost = "0.0.0.0"
    var connectorAddress: String? = null
    var policyPath: String? = null

    val iter = args.iterator()
    while (iter.hasNext()) {
        when (iter.next()) {
            "-l", "--local" -> localHost = iter.next()
            "-c", "--connector" -> connectorAddress = iter.next()
            "-p", "--policy" -> policyPath = iter.next()
            "-h", "--help" -> {
                System.err.println(usage)
                exitProcess(0)
            }
            else -> {
                System.err.println(usage)
                exitProcess(1)
            }
        }
    }

    if (connectorAddress == null || policyPath == null) {
        System.err.println(usage)
        exitProcess(1)
    }

    val connectorUrl =
        run {
            val address = if (connectorAddress.contains("://")) connectorAddress else "http://$connectorAddress"
            val builder = URLBuilder(address)

            if (builder.pathSegments.isEmpty()) {
                builder.path("negotiations")
            }

            builder.build()
        }

    val policy: OdrlOffer =
        if (policyPath == "-") {
            System.`in`
        } else {
            File(policyPath).inputStream()
        }.use(objectMapper::readValue)

    runBlocking {
        val httpClient =
            HttpClient(CIO) {
                install(ClientContentNegotiation) {
                    register(ContentType.Application.Json, JacksonConverter(objectMapper))
                }
                install(Auth) {
                    // TODO: Revisit once authentication is final
                    basic {
                        credentials {
                            BasicAuthCredentials("user", "password")
                        }
                    }
                }
            }
        val appConfig = MapApplicationConfig("connector.identity" to "https://contract-registry-tool.local")
        val configManager = ConfigManager(appConfig)
        val connectorConfig = configManager.getConfig(::ConnectorConfig)
        val negotiationManager =
            BuiltinNegotiationManager(
                httpClient,
                objectMapper,
                DummyNegotiator(true),
                ContractStore(objectMapper),
                OdrlParser(),
                configManager,
            )

        val closeChan = Channel<Unit>()
        val addressChan = Channel<String>()

        launch {
            val server =
                embeddedServer(
                    Netty,
                    environment =
                        applicationEnvironment {
                            config = MapApplicationConfig()
                        },
                    configure = {
                        connector {
                            host = localHost
                            port = 0
                        }
                    },
                    module = {
                        configureAuthentication()
                        install(ServerContentNegotiation) {
                            register(ContentType.Application.Json, JacksonConverter(objectMapper))
                        }
                        routing {
                            BuiltinNegotiationController(negotiationManager, connectorConfig).apply { registerRoutes() }
                        }
                    },
                ).start(wait = false)

            addressChan.send(server.engine.resolvedConnectors().first().run { "http://$host:$port" })
            closeChan.receive()

            server.stop()
        }

        val localAddress = addressChan.receive()
        val negotiationFuture =
            negotiationManager.startNegotiation(
                connectorUrl,
                URLBuilder(localAddress).appendPathSegments("negotiations").build(),
                policy,
            )

        val negotiatedContract = negotiationFuture.await()
        val pretty = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(negotiatedContract)
        println("Successfully negotiated policy $pretty")
        closeChan.send(Unit)
    }
    // For some reason, the gradle run task lingers for some time at this point
    // I suppose that netty still has a few listener threads alive, which take a long time to stop for some reason
    exitProcess(0)
}
