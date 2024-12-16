package de.fraunhofer.aisec.glue.tools

import com.apicatalog.jsonld.JsonLdOptions
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.fasterxml.jackson.module.kotlin.readValue
import de.fraunhofer.aisec.glue.controllers.NegotiationController
import de.fraunhofer.aisec.glue.jsonld.JsonLDModule
import de.fraunhofer.aisec.glue.negotiation.DummyNegotiator
import de.fraunhofer.aisec.glue.negotiation.NegotiationManager
import de.fraunhofer.aisec.glue.plugins.configureAuthentication
import de.fraunhofer.aisec.glue.plugins.configureSerialization
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val usage = "Usage: [-l localAddress] -c connectorAddress -p policyPath"
    val objectMapper = jacksonMapperBuilder().addModules(JavaTimeModule(), JsonLDModule(JsonLdOptions())).build()

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

    val policy: JsonNode =
        if (policyPath == "-") {
            System.`in`
        } else {
            File(policyPath).inputStream()
        }.use(objectMapper::readValue)

    runBlocking {
        val httpClient =
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
        val negotiationManager = NegotiationManager(httpClient, objectMapper, DummyNegotiator(true))

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
                        configureSerialization()
                        routing {
                            NegotiationController(negotiationManager).apply { registerRoutes() }
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
