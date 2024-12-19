package de.fraunhofer.aisec.glue

import com.apicatalog.jsonld.JsonLd
import com.apicatalog.jsonld.JsonLdOptions
import com.apicatalog.jsonld.document.JsonDocument
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.datatype.jsonp.JSONPModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import de.fraunhofer.aisec.glue.controllers.Controller
import de.fraunhofer.aisec.glue.controllers.NegotiationController
import de.fraunhofer.aisec.glue.jsonld.JsonLDModule
import de.fraunhofer.aisec.glue.models.negotiation.*
import de.fraunhofer.aisec.glue.negotiation.DummyNegotiator
import de.fraunhofer.aisec.glue.negotiation.NegotiationManager
import de.fraunhofer.aisec.glue.plugins.configureSerialization
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.mock.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import jakarta.json.JsonObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.slf4j.event.Level
import java.io.File
import java.time.Duration
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.toKotlinDuration

class NegotiationTest {
    private fun httpClient(mockFactory: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
        HttpClient(MockEngine(mockFactory)) {
            install(ContentNegotiation) {
                jackson(streamRequestBody = false) {
                    registerModules(JsonLDModule(JsonLdOptions()))
                }
            }
        }

    private suspend inline fun <reified T> convertValue(body: OutgoingContent): T {
        val raw = body.toByteArray()
        val res = objectMapper.readValue(raw, T::class.java)
        return res
    }

    @Test
    fun `Test negotiation as provider`() =
        runTest(timeout = Duration.ofSeconds(30).toKotlinDuration()) {
            val consumerPid = "urn:uuid:${UUID.randomUUID()}"
            var consumerState = NegotiationState.REQUESTED
            val httpClient =
                httpClient { req ->
                    when (req.url.encodedPath) {
                        "/negotiations/$consumerPid/agreement" -> {
                            val reqBody = convertValue<ContractAgreementMessage>(req.body)
                            // TODO: Verify the body once it serializes correctly
                            consumerState = NegotiationState.AGREED
                            respond("")
                        }
                        "/negotiations/$consumerPid/events" -> {
                            val reqBody = convertValue<ContractNegotiationEventMessage>(req.body)
                            assertEquals(NegotiationEvent.FINALIZED, reqBody.eventType)
                            consumerState = NegotiationState.FINALIZED
                            respond("")
                        }
                        else -> {
                            throw IllegalStateException("Unexpected URL ${req.url} called")
                        }
                    }
                }
            val contractStore = mockk<ContractStore>()
            coJustRun { contractStore.addContract(any()) }

            val negotiationManager = NegotiationManager(httpClient, objectMapper, DummyNegotiator(true), contractStore)

            val contractRequest =
                ContractRequestMessage(
                    providerPid = null,
                    consumerPid = consumerPid,
                    callbackAddress = "https://consumer/negotiations",
                    offer = parsedExamplePolicy,
                )

            val negotiation =
                negotiationManager.createFromRequest(
                    contractRequest,
                    Url("https://provider/negotiations"),
                    "consumer",
                )
            assertNotNull(negotiation)

            var responseCallbackCalled = false
            negotiation.processRequest(contractRequest, initial = true) { ok ->
                assertTrue(ok)
                responseCallbackCalled = true
            }
            assertTrue(responseCallbackCalled)
            assertEquals(NegotiationState.AGREED, negotiation.state)
            assertEquals(NegotiationState.AGREED, consumerState)

            responseCallbackCalled = false
            negotiation.processVerify { ok ->
                assertTrue(ok)
                responseCallbackCalled = true
            }

            assertTrue(responseCallbackCalled)
            assertEquals(NegotiationState.FINALIZED, negotiation.state)
            assertEquals(NegotiationState.FINALIZED, consumerState)

            // Verify that the returned and stored policies are the same
            coVerify { contractStore.addContract(any()) }

            // TODO: Can I structurally compare json nodes?
        }

    @Test
    fun `Test negotiation as consumer`() =
        runTest(timeout = Duration.ofSeconds(30).toKotlinDuration()) {
            val providerPid = "urn:uuid:${UUID.randomUUID()}"
            var providerState = NegotiationState.OFFERED
            val httpClient =
                httpClient { req ->
                    when (req.url.encodedPath) {
                        "/negotiations/$providerPid/events" -> {
                            val reqBody = convertValue<ContractNegotiationEventMessage>(req.body)
                            assertEquals(NegotiationEvent.ACCEPTED, reqBody.eventType)
                            providerState = NegotiationState.ACCEPTED
                            respond("")
                        }

                        "/negotiations/$providerPid/agreement/verification" -> {
                            providerState = NegotiationState.VERIFIED
                            respond("")
                        }
                        else -> {
                            throw IllegalStateException("Unexpected URL ${req.url} called")
                        }
                    }
                }
            val contractStore = mockk<ContractStore>()
            coJustRun { contractStore.addContract(any()) }
            val negotiationManager = NegotiationManager(httpClient, objectMapper, DummyNegotiator(), contractStore)

            val contractOffer =
                ContractOfferMessage(
                    providerPid = providerPid,
                    consumerPid = null,
                    callbackAddress = "https://provider/negotiations",
                    offer = parsedExamplePolicy,
                )

            val negotiation =
                negotiationManager.createFromOffer(
                    contractOffer,
                    Url("https://consumer/negotiations"),
                    "provider",
                )
            assertNotNull(negotiation)

            var responseCallbackCalled = false
            negotiation.processOffer(contractOffer, initial = true) { ok ->
                assertTrue(ok)
                responseCallbackCalled = true
            }

            assertTrue(responseCallbackCalled)
            assertEquals(NegotiationState.ACCEPTED, negotiation.state)
            assertEquals(NegotiationState.ACCEPTED, providerState)

            val agreementReq =
                ContractAgreementMessage(
                    providerPid = providerPid,
                    consumerPid = negotiation.ownPid,
                    agreement = parsedExamplePolicy,
                    callbackAddress = "https://provider/negotiations",
                )
            responseCallbackCalled = false
            negotiation.processAgreement(agreementReq) { ok ->
                assertTrue(ok)
                responseCallbackCalled = true
            }

            assertTrue(responseCallbackCalled)
            assertEquals(NegotiationState.VERIFIED, negotiation.state)
            assertEquals(NegotiationState.VERIFIED, providerState)

            responseCallbackCalled = false
            negotiation.processFinalize { ok ->
                assertTrue(ok)
                responseCallbackCalled = true
            }

            assertTrue(responseCallbackCalled)
            assertEquals(NegotiationState.FINALIZED, negotiation.state)

            coVerify { contractStore.addContract(any()) }
        }

    private fun ApplicationTestBuilder.configureTestApp(controller: Controller) {
        environment {
            // We want to configure the application modules ourselves
            config = ApplicationConfig("/dev/null")
        }
        application {
            configureSerialization()
            install(Authentication) {
                basic {
                    validate { creds -> UserIdPrincipal(creds.name) }
                }
            }
            routing {
                controller.apply { registerRoutes() }
            }
        }
    }

    private fun ApplicationTestBuilder.testClient() =
        createClient {
            install(ContentNegotiation) {
                jackson {
                    registerModules(JsonLDModule(JsonLdOptions()))
                }
            }
        }

    @Test
    fun `Test negotiation as provider over HTTP`() =
        testApplication {
            val consumerPid = "urn:uuid:${UUID.randomUUID()}"
            var consumerState = NegotiationState.REQUESTED
            val httpClient =
                httpClient { req ->
                    when (req.url.encodedPath) {
                        "/negotiations/$consumerPid/agreement" -> {
                            val reqBody = convertValue<ContractAgreementMessage>(req.body)
                            // TODO: Verify the body once it serializes correctly
                            consumerState = NegotiationState.AGREED
                            respond("")
                        }
                        "/negotiations/$consumerPid/events" -> {
                            val reqBody = convertValue<ContractNegotiationEventMessage>(req.body)
                            assertEquals(NegotiationEvent.FINALIZED, reqBody.eventType)
                            consumerState = NegotiationState.FINALIZED
                            respond("")
                        }
                        else -> {
                            throw IllegalStateException("Unexpected URL ${req.url} called")
                        }
                    }
                }
            val contractStore = ContractStore(objectMapper)
            val negotiationManager = NegotiationManager(httpClient, objectMapper, DummyNegotiator(), contractStore)
            val controller = NegotiationController(negotiationManager)

            configureTestApp(controller)
            val testClient = testClient()

            var resp =
                testClient.post("https://localhost/negotiations/request") {
                    basicAuth("consumer", "password")
                    contentType(ContentType.Application.Json)
                    setBody(
                        ContractRequestMessage(
                            providerPid = null,
                            consumerPid = consumerPid,
                            callbackAddress = "https://consumer/negotiations",
                            offer = parsedExamplePolicy,
                        ),
                    )
                }

            assertTrue(resp.status.isSuccess())
            val respBody = resp.body<ContractNegotiation>()
            assertEquals(NegotiationState.REQUESTED, respBody.state)

            // This should make sure that the request has been fully processed and the negotiation has called the client
            delay(100)

            val negotiation = negotiationManager.getAsProvider(respBody.providerPid, consumerPid, "consumer")
            assertNotNull(negotiation)
            assertEquals(NegotiationState.AGREED, negotiation.state)
            assertEquals(NegotiationState.AGREED, consumerState)

            resp =
                testClient.post("https://localhost/negotiations/${respBody.providerPid}/agreement/verification") {
                    basicAuth("consumer", "password")
                    contentType(ContentType.Application.Json)
                    setBody(
                        ContractAgreementVerificationMessage(
                            providerPid = respBody.providerPid,
                            consumerPid = consumerPid,
                        ),
                    )
                }
            assertTrue(resp.status.isSuccess())
            assertEquals(NegotiationState.FINALIZED, negotiation.state)
            assertEquals(NegotiationState.FINALIZED, consumerState)
        }

    @Test
    fun `Test negotiation as consumer over HTTP`() =
        testApplication {
            val providerPid = "urn:uuid:${UUID.randomUUID()}"
            var providerState = NegotiationState.OFFERED
            val httpClient =
                httpClient { req ->
                    when (req.url.encodedPath) {
                        "/negotiations/$providerPid/events" -> {
                            val reqBody = convertValue<ContractNegotiationEventMessage>(req.body)
                            assertEquals(NegotiationEvent.ACCEPTED, reqBody.eventType)
                            providerState = NegotiationState.ACCEPTED
                            respond("")
                        }

                        "/negotiations/$providerPid/agreement/verification" -> {
                            providerState = NegotiationState.VERIFIED
                            respond("")
                        }
                        else -> {
                            throw IllegalStateException("Unexpected URL ${req.url} called")
                        }
                    }
                }
            val contractStore = mockk<ContractStore>()
            coJustRun { contractStore.addContract(any()) }
            val negotiationManager = NegotiationManager(httpClient, objectMapper, DummyNegotiator(), contractStore)
            val controller = NegotiationController(negotiationManager)

            configureTestApp(controller)
            val testClient = testClient()

            var resp =
                testClient.post("https://localhost/negotiations/offers") {
                    basicAuth("provider", "password")
                    contentType(ContentType.Application.Json)
                    setBody(
                        ContractOfferMessage(
                            providerPid = providerPid,
                            consumerPid = null,
                            callbackAddress = "https://provider/negotiations",
                            offer = parsedExamplePolicy,
                        ),
                    )
                }

            assertTrue(resp.status.isSuccess())
            val respBody = resp.body<ContractNegotiation>()
            assertEquals(NegotiationState.OFFERED, respBody.state)

            // This should make sure that the request has been fully processed and the negotiation has called the client
            delay(100)

            val negotiation = negotiationManager.getAsConsumer(respBody.consumerPid, providerPid, "provider")
            assertNotNull(negotiation)
            assertEquals(NegotiationState.ACCEPTED, negotiation.state)
            assertEquals(NegotiationState.ACCEPTED, providerState)

            resp =
                testClient.post("https://localhost/negotiations/${respBody.consumerPid}/agreement") {
                    basicAuth("provider", "password")
                    contentType(ContentType.Application.Json)
                    setBody(
                        ContractAgreementMessage(
                            providerPid = providerPid,
                            consumerPid = respBody.consumerPid,
                            agreement = parsedExamplePolicy,
                            callbackAddress = "https://consumer/negotiations",
                        ),
                    )
                }
            assertTrue(resp.status.isSuccess())

            delay(100)

            assertEquals(NegotiationState.VERIFIED, negotiation.state)
            assertEquals(NegotiationState.VERIFIED, providerState)

            resp =
                testClient.post("https://localhost/negotiations/${respBody.consumerPid}/events") {
                    basicAuth("provider", "password")
                    contentType(ContentType.Application.Json)
                    setBody(
                        ContractNegotiationEventMessage(
                            providerPid = providerPid,
                            consumerPid = respBody.consumerPid,
                            eventType = NegotiationEvent.FINALIZED,
                        ),
                    )
                }
            assertTrue(resp.status.isSuccess())
            assertEquals(NegotiationState.FINALIZED, negotiation.state)
        }

    @Test
    fun `Test mutual contract negotiation`() =
        runTest(timeout = Duration.ofSeconds(30).toKotlinDuration()) {
            val testClient =
                HttpClient(OkHttp) {
                    install(ContentNegotiation) {
                        jackson {
                            registerModules(JsonLDModule(JsonLdOptions()))
                        }
                    }
                    install(Auth) {
                        basic {
                            credentials {
                                BasicAuthCredentials("connector", "password")
                            }
                            sendWithoutRequest { true }
                        }
                    }
                }

            val providerContractStore = mockk<ContractStore>()
            coJustRun { providerContractStore.addContract(any()) }

            val providerNegotiationManager = NegotiationManager(testClient, objectMapper, DummyNegotiator(), providerContractStore)
            val providerController = NegotiationController(providerNegotiationManager)

            val consumerContractStore = mockk<ContractStore>()
            coJustRun { consumerContractStore.addContract(any()) }

            val consumerNegotiationManager = NegotiationManager(testClient, objectMapper, DummyNegotiator(), consumerContractStore)
            val consumerController = NegotiationController(consumerNegotiationManager)

            fun testServer(
                role: String,
                controller: NegotiationController,
            ) = embeddedServer(
                Netty,
                environment =
                    applicationEnvironment {
                        config = MapApplicationConfig("ktor.deployment.parallelism" to "1")
                    },
                configure = {
                    connector {
                        host = "localhost"
                        port = 0
                    }
                },
                module = {
                    configureSerialization()
                    install(Authentication) {
                        basic {
                            validate { creds -> UserIdPrincipal(creds.name) }
                        }
                    }
                    install(CallLogging) {
                        level = Level.INFO
                        format { call ->
                            "$role: ${call.request.httpMethod.value} ${call.request.path()} - ${call.response.status()}"
                        }
                    }
                    routing {
                        controller.apply { registerRoutes() }
                    }
                },
            )

            val provider = testServer("provider", providerController).start(false)
            val providerAddress = provider.engine.resolvedConnectors().first().run { "http://$host:$port" }

            val consumer = testServer("consumer", consumerController).start(false)
            val consumerAddress = consumer.engine.resolvedConnectors().first().run { "http://$host:$port" }

            // This indirectly checks if the consumer side negotiation completed successfully
            val negotiatedPolicy =
                consumerNegotiationManager.startNegotiation(
                    URLBuilder(providerAddress).appendPathSegments("negotiations").build(),
                    URLBuilder(consumerAddress).appendPathSegments("negotiations").build(),
                    parsedExamplePolicy,
                    null,
                ).await()

            coVerify { providerContractStore.addContract(negotiatedPolicy) }
            coVerify { consumerContractStore.addContract(negotiatedPolicy) }
        }

    // TODO: Add tests that try incorrect or behavior as well
    // TODO: Technically, we may also want to test using a custom negotiator to cover all code paths

    companion object {
        private val objectMapper =
            jacksonMapperBuilder()
                .addModules(JsonLDModule(JsonLdOptions()), JSONPModule(), JavaTimeModule())
                .build()

        val parsedExamplePolicy: JsonNode by lazy {
            File("test-containers/example-policy.json").inputStream().use { input ->
                val mapper = jacksonMapperBuilder().addModules(JSONPModule(), JavaTimeModule()).build()
                val jsonld = mapper.readValue(input, JsonObject::class.java)
                val compacted =
                    JsonLd.compact(JsonDocument.of(jsonld), JsonDocument.of(JsonObject.EMPTY_JSON_OBJECT)).get()
                mapper.convertValue(compacted, JsonNode::class.java)
            }
        }
    }
}
