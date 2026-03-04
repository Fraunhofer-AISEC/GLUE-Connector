package de.fraunhofer.aisec.glue

import de.fraunhofer.aisec.glue.config.ConfigManager
import de.fraunhofer.aisec.glue.config.ConnectorConfig
import de.fraunhofer.aisec.glue.controllers.AtlsController
import de.fraunhofer.aisec.glue.controllers.BuiltinNegotiationController
import de.fraunhofer.aisec.glue.models.negotiation.*
import de.fraunhofer.aisec.glue.negotiation.builtin.BuiltinNegotiationManager
import de.fraunhofer.aisec.glue.negotiation.builtin.DummyNegotiator
import de.fraunhofer.aisec.glue.negotiation.toAgreement
import de.fraunhofer.iosb.odrl.OdrlOffer
import de.fraunhofer.iosb.odrl.OdrlParty
import de.fraunhofer.iosb.odrl.parser.OdrlParser
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.slf4j.event.Level
import java.io.File
import java.time.Duration
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertIsNot
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.toKotlinDuration
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

class NegotiationTest {
    private fun httpClient(mockFactory: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
        HttpClient(MockEngine(mockFactory)) {
            install(ClientContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter(objectMapper, streamRequestBody = false))
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
            val agreementLatch = CompletableDeferred<Unit>()
            val finalizedLatch = CompletableDeferred<Unit>()
            val httpClient =
                httpClient { req ->
                    when (req.url.encodedPath) {
                        "/negotiations/$consumerPid/agreement" -> {
                            convertValue<ContractAgreementMessage>(req.body)
                            agreementLatch.complete(Unit)
                            respond("")
                        }
                        "/negotiations/$consumerPid/events" -> {
                            val reqBody = convertValue<ContractNegotiationEventMessage>(req.body)
                            assertEquals(NegotiationEvent.FINALIZED, reqBody.eventType)
                            finalizedLatch.complete(Unit)
                            respond("")
                        }
                        else -> {
                            throw IllegalStateException("Unexpected URL ${req.url} called")
                        }
                    }
                }
            val contractStore = mockk<ContractStore>()
            coJustRun { contractStore.addContract(any()) }

            val configManager = ConfigManager(MapApplicationConfig("connector.identity" to "https://test-connector.local"))
            val negotiationManager =
                BuiltinNegotiationManager(
                    httpClient,
                    objectMapper,
                    DummyNegotiator(true),
                    contractStore,
                    OdrlParser(),
                    configManager,
                )

            val contractRequest =
                ContractRequestMessage(
                    uid = "urn:uuid:${UUID.randomUUID()}",
                    providerPid = null,
                    consumerPid = consumerPid,
                    callbackAddress = "https://consumer",
                    offer = exampleOffer,
                )

            val negotiation =
                negotiationManager.createFromRequest(
                    contractRequest,
                    "consumer",
                )
            assertNotNull(negotiation)

            val stateMachineObserver =
                StateMachineObserver(
                    NegotiationState.INITIAL,
                    NegotiationState.REQUESTED,
                    NegotiationState.AGREED,
                    NegotiationState.VERIFIED,
                    NegotiationState.FINALIZED,
                )
            stateMachineObserver.record(this, negotiation.data.map { it.state })

            val initialResp = negotiation.submitMessage(contractRequest)
            assertIs<ContractNegotiation>(initialResp)

            agreementLatch.await()

            val verifyReq = ContractAgreementVerificationMessage(providerPid = initialResp.providerPid, consumerPid = consumerPid)
            val verifyResp = negotiation.submitMessage(verifyReq)
            assertIsNot<ContractNegotiationError>(verifyResp)

            finalizedLatch.await()

            stateMachineObserver.verify()

            // Verify that the returned and stored policies are the same
            coVerify { contractStore.addContract(any()) }

            // TODO: Can I structurally compare json nodes?
        }

    @Test
    fun `Test negotiation as consumer`() =
        runTest(timeout = Duration.ofSeconds(30).toKotlinDuration()) {
            val providerPid = "urn:uuid:${UUID.randomUUID()}"
            val acceptedLatch = CompletableDeferred<Unit>()
            val verifiedLatch = CompletableDeferred<Unit>()
            val httpClient =
                httpClient { req ->
                    when (req.url.encodedPath) {
                        "/negotiations/$providerPid/events" -> {
                            val reqBody = convertValue<ContractNegotiationEventMessage>(req.body)
                            assertEquals(NegotiationEvent.ACCEPTED, reqBody.eventType)
                            acceptedLatch.complete(Unit)
                            respond("")
                        }

                        "/negotiations/$providerPid/agreement/verification" -> {
                            verifiedLatch.complete(Unit)
                            respond("")
                        }

                        else -> {
                            throw IllegalStateException("Unexpected URL ${req.url} called")
                        }
                    }
                }
            val contractStore = mockk<ContractStore>()
            coJustRun { contractStore.addContract(any()) }

            val configManager = ConfigManager(MapApplicationConfig("connector.identity" to "https://test-connector.local"))
            val negotiationManager =
                BuiltinNegotiationManager(
                    httpClient,
                    objectMapper,
                    DummyNegotiator(),
                    contractStore,
                    OdrlParser(),
                    configManager,
                )

            val contractOffer =
                ContractOfferMessage(
                    providerPid = providerPid,
                    consumerPid = null,
                    callbackAddress = "https://provider",
                    offer = exampleOffer,
                )

            val negotiation =
                negotiationManager.createFromOffer(
                    contractOffer,
                    "provider",
                )
            assertNotNull(negotiation)

            val stateMachineObserver =
                StateMachineObserver(
                    NegotiationState.INITIAL,
                    NegotiationState.OFFERED,
                    NegotiationState.ACCEPTED,
                    NegotiationState.AGREED,
                    NegotiationState.VERIFIED,
                    NegotiationState.FINALIZED,
                )
            stateMachineObserver.record(this, negotiation.data.map { it.state })

            val initialResp = negotiation.submitMessage(contractOffer)
            assertIs<ContractNegotiation>(initialResp)

            acceptedLatch.await()

            val agreementReq =
                ContractAgreementMessage(
                    providerPid = providerPid,
                    consumerPid = negotiation.id,
                    agreement = exampleAgreement,
                    callbackAddress = "https://provider/negotiations",
                )

            val agreementResp = negotiation.submitMessage(agreementReq)
            assertIsNot<ContractNegotiationError>(agreementResp)

            verifiedLatch.await()

            val finalizeReq =
                ContractNegotiationEventMessage(
                    providerPid = providerPid,
                    consumerPid = initialResp.consumerPid,
                    eventType = NegotiationEvent.FINALIZED,
                )
            val finalizeResp = negotiation.submitMessage(finalizeReq)
            assertIsNot<ContractNegotiationError>(finalizeResp)

            stateMachineObserver.verify()

            coVerify { contractStore.addContract(any()) }
        }

    private fun ApplicationTestBuilder.configureTestApp(controller: AtlsController) {
        environment {
            // We want to configure the application modules ourselves
            config = ApplicationConfig("/dev/null")
        }
        application {
            install(ServerContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter(objectMapper))
            }
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
            install(ClientContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter(objectMapper))
            }
        }

    @Test
    fun `Test negotiation as provider over HTTP`() =
        testApplication {
            val consumerPid = "urn:uuid:${UUID.randomUUID()}"
            val agreedLatch = CompletableDeferred<Unit>()
            val finalizedLatch = CompletableDeferred<Unit>()
            val httpClient =
                httpClient { req ->
                    when (req.url.encodedPath) {
                        "/negotiations/$consumerPid/agreement" -> {
                            convertValue<ContractAgreementMessage>(req.body)
                            agreedLatch.complete(Unit)
                            respond("")
                        }

                        "/negotiations/$consumerPid/events" -> {
                            val reqBody = convertValue<ContractNegotiationEventMessage>(req.body)
                            assertEquals(NegotiationEvent.FINALIZED, reqBody.eventType)
                            finalizedLatch.complete(Unit)
                            respond("")
                        }

                        else -> {
                            throw IllegalStateException("Unexpected URL ${req.url} called")
                        }
                    }
                }
            val contractStore = ContractStore(objectMapper)
            val configManager = ConfigManager(MapApplicationConfig("connector.identity" to "https://test-connector.local"))
            val negotiationManager =
                BuiltinNegotiationManager(
                    httpClient,
                    objectMapper,
                    DummyNegotiator(),
                    contractStore,
                    OdrlParser(),
                    configManager,
                )
            val controller = BuiltinNegotiationController(negotiationManager, configManager.getConfig(::ConnectorConfig))

            configureTestApp(controller)
            val testClient = testClient()

            var resp =
                testClient.post("https://localhost/negotiations/request") {
                    basicAuth("consumer", "password")
                    contentType(ContentType.Application.Json)
                    setBody(
                        ContractRequestMessage(
                            uid = "urn:uuid:${UUID.randomUUID()}",
                            providerPid = null,
                            consumerPid = consumerPid,
                            callbackAddress = "https://consumer",
                            offer = exampleOffer,
                        ),
                    )
                }

            assertTrue(resp.status.isSuccess())
            val respBody = resp.body<ContractNegotiation>()
            assertEquals(NegotiationState.REQUESTED, respBody.state)

            agreedLatch.await()

            val negotiation = negotiationManager.getAsProvider(respBody.providerPid, consumerPid, "consumer")
            assertNotNull(negotiation)

            val observer = StateMachineObserver(NegotiationState.AGREED, NegotiationState.VERIFIED, NegotiationState.FINALIZED)
            val scope = CoroutineScope(currentCoroutineContext())
            observer.record(scope, negotiation.data.map { it.state })

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

            finalizedLatch.await()
            observer.verify()
        }

    @Test
    fun `Test negotiation as consumer over HTTP`() =
        testApplication {
            val providerPid = "urn:uuid:${UUID.randomUUID()}"
            val acceptedLatch = CompletableDeferred<Unit>()
            val verificationLatch = CompletableDeferred<Unit>()
            val httpClient =
                httpClient { req ->
                    when (req.url.encodedPath) {
                        "/negotiations/$providerPid/events" -> {
                            val reqBody = convertValue<ContractNegotiationEventMessage>(req.body)
                            assertEquals(NegotiationEvent.ACCEPTED, reqBody.eventType)
                            acceptedLatch.complete(Unit)
                            respond("")
                        }

                        "/negotiations/$providerPid/agreement/verification" -> {
                            verificationLatch.complete(Unit)
                            respond("")
                        }

                        else -> {
                            throw IllegalStateException("Unexpected URL ${req.url} called")
                        }
                    }
                }
            val contractStore = mockk<ContractStore>()
            coJustRun { contractStore.addContract(any()) }
            val configManager = ConfigManager(MapApplicationConfig("connector.identity" to "https://test-connector.local"))
            val negotiationManager =
                BuiltinNegotiationManager(
                    httpClient,
                    objectMapper,
                    DummyNegotiator(),
                    contractStore,
                    OdrlParser(),
                    configManager,
                )
            val controller = BuiltinNegotiationController(negotiationManager, configManager.getConfig(::ConnectorConfig))

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
                            callbackAddress = "https://provider",
                            offer = exampleOffer,
                        ),
                    )
                }

            assertTrue(resp.status.isSuccess())
            val respBody = resp.body<ContractNegotiation>()
            assertEquals(NegotiationState.OFFERED, respBody.state)

            acceptedLatch.await()

            val negotiation = negotiationManager.getAsConsumer(respBody.consumerPid, providerPid, "provider")
            assertNotNull(negotiation)

            val observer =
                StateMachineObserver(
                    NegotiationState.ACCEPTED,
                    NegotiationState.AGREED,
                    NegotiationState.VERIFIED,
                    NegotiationState.FINALIZED,
                )
            val scope = CoroutineScope(currentCoroutineContext())
            observer.record(scope, negotiation.data.map { it.state })

            resp =
                testClient.post("https://localhost/negotiations/${respBody.consumerPid}/agreement") {
                    basicAuth("provider", "password")
                    contentType(ContentType.Application.Json)
                    setBody(
                        ContractAgreementMessage(
                            providerPid = providerPid,
                            consumerPid = respBody.consumerPid,
                            agreement = exampleAgreement,
                            callbackAddress = "https://consumer/negotiations",
                        ),
                    )
                }
            assertTrue(resp.status.isSuccess())

            verificationLatch.await()

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

            observer.verify()
        }

    @Test
    fun `Test mutual contract negotiation`() =
        runBlocking {
            val testClient =
                HttpClient(CIO) {
                    install(ClientContentNegotiation) {
                        register(ContentType.Application.Json, JacksonConverter(objectMapper))
                    }
                    install(Auth) {
                        basic {
                            credentials {
                                BasicAuthCredentials("connector", "password")
                            }
                            // sendWithoutRequest { true }
                        }
                    }
                }

            val odrlParser = OdrlParser()

            val providerContractStore = mockk<ContractStore>()
            coJustRun { providerContractStore.addContract(any()) }

            val providerConfig = MapApplicationConfig("connector.identity" to "https://provider-connector.local")
            val providerConfigManager = ConfigManager(providerConfig)
            val providerNegotiationManager =
                BuiltinNegotiationManager(
                    testClient,
                    objectMapper,
                    DummyNegotiator(),
                    providerContractStore,
                    odrlParser,
                    providerConfigManager,
                )
            val providerController =
                BuiltinNegotiationController(providerNegotiationManager, providerConfigManager.getConfig(::ConnectorConfig))

            val consumerContractStore = mockk<ContractStore>()
            coJustRun { consumerContractStore.addContract(any()) }

            val consumerConfig = MapApplicationConfig("connector.identity" to "https://consumer-connector.local")
            val consumerConfigManager = ConfigManager(consumerConfig)
            val consumerNegotiationManager =
                BuiltinNegotiationManager(
                    testClient,
                    objectMapper,
                    DummyNegotiator(),
                    consumerContractStore,
                    odrlParser,
                    consumerConfigManager,
                )
            val consumerController =
                BuiltinNegotiationController(consumerNegotiationManager, consumerConfigManager.getConfig(::ConnectorConfig))

            fun testServer(
                role: String,
                controller: BuiltinNegotiationController,
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
                    install(ServerContentNegotiation) {
                        register(ContentType.Application.Json, JacksonConverter(objectMapper))
                    }
                    install(Authentication) {
                        basic {
                            skipWhen { true }
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
            // We can still set this config option since options are evaluated lazily
            providerConfig.put("connector.publicUrl", providerAddress)

            val consumer = testServer("consumer", consumerController).start(false)
            val consumerAddress = consumer.engine.resolvedConnectors().first().run { "http://$host:$port" }
            consumerConfig.put("connector.publicUrl", consumerAddress)

            // This indirectly checks if the consumer side negotiation completed successfully
            val negotiatedPolicy =
                withTimeout(Duration.ofSeconds(30).toKotlinDuration()) {
                    consumerNegotiationManager.startNegotiation(
                        providerAddress,
                        exampleOffer,
                    ).await()
                }

            coVerify { providerContractStore.addContract(negotiatedPolicy) }
            coVerify { consumerContractStore.addContract(negotiatedPolicy) }
        }

    // TODO: Add tests that try incorrect or behavior as well
    // TODO: Technically, we may also want to test using a custom negotiator to cover all code paths

    class StateMachineObserver(vararg val expectedStates: NegotiationState) {
        private lateinit var recordJob: Job
        private val actualStates = mutableListOf<NegotiationState>()

        fun record(
            scope: CoroutineScope,
            flow: Flow<NegotiationState>,
        ) {
            recordJob =
                scope.launch {
                    var current = flow.first()
                    actualStates.add(current)
                    while (true) {
                        val next = flow.first { it != current }
                        actualStates.add(next)
                        if (next == NegotiationState.FINALIZED || next == NegotiationState.TERMINATED) {
                            break
                        }
                        current = next
                    }
                }
        }

        suspend fun verify() {
            recordJob.join()
            assertEquals(expectedStates.toList(), actualStates)
        }
    }

    companion object {
        private val objectMapper = OdrlParser().mapper

        val exampleOffer: OdrlOffer =
            File("test-containers/example-policy.json").inputStream().use { input ->
                objectMapper.readValue(input, OdrlOffer::class.java)
            }

        val exampleAgreement = exampleOffer.toAgreement(OdrlParser(), OdrlParty("http://dataconsumer.com/org"))
    }
}
