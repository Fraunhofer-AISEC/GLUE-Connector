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
import io.ktor.server.plugins.callloging.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import jakarta.json.JsonObject
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.slf4j.event.Level
import java.time.Duration
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.toKotlinDuration

class NegotiationTest {
    private fun httpClient(mockFactory: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
        HttpClient(MockEngine(mockFactory)) {
            install(ContentNegotiation) {
                jackson {
                    registerModules(JsonLDModule(JsonLdOptions()))
                }
            }
        }

    private inline fun <reified T> convertValue(body: OutgoingContent): T =
        runBlocking {
            val raw = body.toByteArray()
            objectMapper.readValue(raw, T::class.java)
        }

    @Test
    fun `Test negotiation as provider`() =
        runBlocking {
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
            val negotiationManager = NegotiationManager(httpClient, objectMapper, DummyNegotiator())

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

            assertTrue(negotiation.completed.isCompleted)
            val result = negotiation.completed.await()
            // TODO: Check if the policy is still the same
        }

    @Test
    fun `Test negotiation as consumer`() =
        runBlocking {
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
            val negotiationManager = NegotiationManager(httpClient, objectMapper, DummyNegotiator())

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

            assertTrue(negotiation.completed.isCompleted)
            val result = negotiation.completed.await()
            // TODO: Check if the policy is still the same
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
            install(Routing) {
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
            val negotiationManager = NegotiationManager(httpClient, objectMapper, DummyNegotiator())
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
            val negotiationManager = NegotiationManager(httpClient, objectMapper, DummyNegotiator())
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

            val providerNegotiationManager = NegotiationManager(testClient, objectMapper, DummyNegotiator())
            val providerNegotiationFuture = async { providerNegotiationManager.negotiatedContracts.first() }
            val providerController = NegotiationController(providerNegotiationManager)

            val consumerNegotiationManager = NegotiationManager(testClient, objectMapper, DummyNegotiator())
            val consumerController = NegotiationController(consumerNegotiationManager)

            fun appEnv(
                role: String,
                controller: NegotiationController,
            ) = applicationEngineEnvironment {
                module {
                    configureSerialization()
                    install(Authentication) {
                        basic {
                            validate { creds -> UserIdPrincipal(creds.name) }
                        }
                    }
                    install(Routing) {
                        controller.apply { registerRoutes() }
                    }
                    install(CallLogging) {
                        level = Level.INFO
                        format { call ->
                            "$role: ${call.request.httpMethod.value} ${call.request.path()} - ${call.response.status()}"
                        }
                    }
                }
                connector {
                    host = "localhost"
                    port = 0
                }
                developmentMode = true
                config =
                    MapApplicationConfig(
                        "ktor.deployment.parallelism" to "1",
                    )
            }

            val provider = embeddedServer(Netty, appEnv("provider", providerController)).start(false)
            val providerAddress = provider.resolvedConnectors().first().run { "http://$host:$port" }

            val consumer = embeddedServer(Netty, appEnv("consumer", consumerController)).start(false)
            val consumerAddress = consumer.resolvedConnectors().first().run { "http://$host:$port" }

            // This indirectly checks if the consumer side negotiation completed successfully
            val negotiatedPolicy =
                consumerNegotiationManager.startNegotiation(
                    URLBuilder(providerAddress).appendPathSegments("negotiations").build(),
                    URLBuilder(consumerAddress).appendPathSegments("negotiations").build(),
                    parsedExamplePolicy,
                    null,
                ).await()

            // Check if the provider-side negotiation completed successfully as well (if not, this will time out)
            providerNegotiationFuture.await()
        }

    // TODO: Add tests that try incorrect or behavior as well
    // TODO: Technically, we may also want to test using a custom negotiator to cover all code paths

    companion object {
        private val objectMapper =
            jacksonMapperBuilder()
                .addModules(JsonLDModule(JsonLdOptions()), JSONPModule(), JavaTimeModule())
                .build()

        private val examplePolicy =
            """
            {
                "@context": "http://www.w3.org/ns/odrl.jsonld",
                "@type": "Policy",
                "uid": "http://example.com/policy:6163",
                "permission": [{
                    "target": "http://example.com/document:1234",
                    "assigner": "http://example.com/org:616",
                    "action": {
                        "@id": "use",
                        "refinement": [{
                            "leftOperand": "http://fhg.de/glue:containerPipeline",
                            "operator": "eq",
                            "rightOperand": {
                                "@type": "http://fhg.de/glue:ContainerPipeline",
                                "containers": [{
                                    "@type": "http://fhg.de/glue:Container",
                                    "image": "test/container:latest",
                                    "hash": "364a4787748a98c1c28085d4154a06c3a9be9a67c0b08a56b3f69fa8fabf172e",
                                    "cmd": "/app",
                                    "dockerArgs": "--network=...",
                                    "allowNetwork": true
                                },
                                {
                                    "@type": "http://fhg.de/glue:Container",
                                    "image": "test/container:latest",
                                    "hash": "364a4787748a98c1c28085d4154a06c3a9be9a67c0b08a56b3f69fa8fabf172e"
                                }]
                            }
                        }]
                    },
                    "constraint": [{
                        "leftOperand": "dateTime",
                        "operator": "lteq",
                        "rightOperand": {"@value": "2024-12-21T00:00", "@type": "xsd:dateTime"}
                    }]
                }],
                "obligation": [{
                    "target": "http://example.com/document:1234",
                    "action": "distribute",
                    "duty": {
                        "action": "nextPolicy",
                        "assignee": "http://example.com/org:1234",
                        "target": "http://example.com/policyTemplate:1234",
                        "refinement": [{
                            "leftOperand": "http://fhg.de/glue:containerPipeline",
                            "operator": "eq",
                            "rightOperand": {
                                "@type": "http://fhg.de/glue:ContainerPipeline",
                                "containers": [{
                                    "@type": "http://fhg.de/glue:Container",
                                    "image": "test/policyCompiler:latest",
                                    "hash": "364a4787748a98c1c28085d4154a06c3a9be9a67c0b08a56b3f69fa8fabf172e"
                                }]
                            }
                        }]
                    }
                }]
            }
            """.trimIndent()

        val parsedExamplePolicy: JsonNode by lazy {
            val mapper = jacksonMapperBuilder().addModules(JSONPModule(), JavaTimeModule()).build()
            val jsonld = mapper.readValue(examplePolicy, JsonObject::class.java)
            val compacted = JsonLd.compact(JsonDocument.of(jsonld), JsonDocument.of(JsonObject.EMPTY_JSON_OBJECT)).get()
            mapper.convertValue(compacted, JsonNode::class.java)
        }
    }
}
