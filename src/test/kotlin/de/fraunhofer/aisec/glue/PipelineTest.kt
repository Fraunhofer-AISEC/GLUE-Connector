package de.fraunhofer.aisec.glue

import com.apicatalog.jsonld.JsonLdOptions
import com.fasterxml.jackson.datatype.jsonp.JSONPModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import de.fraunhofer.aisec.glue.controllers.PipelineController
import de.fraunhofer.aisec.glue.docker.DockerClient
import de.fraunhofer.aisec.glue.jsonld.JsonLDModule
import de.fraunhofer.aisec.glue.models.pipeline.PipelineCreateRequest
import de.fraunhofer.aisec.glue.models.pipeline.PipelineCreateResponse
import de.fraunhofer.aisec.glue.pipeline.*
import de.fraunhofer.aisec.glue.pipeline.docker.DockerBackend
import de.fraunhofer.aisec.glue.plugins.configureSerialization
import de.fraunhofer.iosb.odrl.glue.GlueContainer
import de.fraunhofer.iosb.odrl.glue.GlueContainerPipeline
import de.fraunhofer.iosb.odrl.parser.OdrlParser
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.config.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

class PipelineTest {
    // Baseline test to check if the PipelineManager still behaves as it should
    @Test
    fun `Test pipeline creation from contract`() =
        runTest {
            val mockPipeline = mockk<ContainerPipeline>()

            val containerInfos =
                listOf(
                    ContainerInfo(
                        name = "test-container-1",
                        ipAddresses = listOf("127.0.0.2"),
                        ports = listOf(ContainerPort(8080u, 12345u, "tcp")),
                        status = ContainerInfo.ContainerStatus.HEALTHY,
                    ),
                    ContainerInfo(
                        name = "test-container-2",
                        ipAddresses = listOf("127.0.0.3"),
                        ports = emptyList(),
                        status = ContainerInfo.ContainerStatus.HEALTHY,
                    ),
                )

            coEvery { mockPipeline.getContainerInfo(any()) } answers { containerInfos[firstArg()] }
            coEvery { mockPipeline.getContainerInfo() } returns containerInfos
            coJustRun { mockPipeline.injectConfig(any(), any()) }
            coJustRun { mockPipeline.start() }

            val mockBackend = mockk<Backend>()
            val pipelineName = slot<String>()

            coEvery { mockBackend.createPipeline(capture(pipelineName), any()) } returns mockPipeline
            coEvery { mockBackend.getConnectorIp(any()) } returns "127.0.0.1"

            val pipelineManager = PipelineManager(objectMapper, mockBackend)

            val pipeline = pipelineManager.createPipeline(testContainers)

            assertEquals(mockPipeline, pipeline)

            coVerify(exactly = 1) { mockPipeline.start() }
            coVerify(exactly = 1) { mockPipeline.injectConfig(0, match { c -> c.nextContainer == "test-container-2" }) }
            coVerify(exactly = 1) { mockPipeline.injectConfig(1, match { c -> c.nextContainer == null }) }

            coJustRun { mockPipeline.stop() }
            coJustRun { mockPipeline.remove() }

            pipelineManager.deletePipeline(pipelineName.captured)

            coVerify(exactly = 1) { mockPipeline.stop() }
            coVerify(exactly = 1) { mockPipeline.remove() }
        }

    @Test
    fun `Test pipeline controller over HTTP`() =
        testApplication {
            val mockPipeline = mockk<ContainerPipeline>()

            val containerInfos =
                listOf(
                    ContainerInfo(
                        name = "test-container-1",
                        ipAddresses = listOf("127.0.0.2"),
                        ports = listOf(ContainerPort(8080u, 12345u, "tcp")),
                        status = ContainerInfo.ContainerStatus.HEALTHY,
                    ),
                    ContainerInfo(
                        name = "test-container-2",
                        ipAddresses = listOf("127.0.0.3"),
                        ports = emptyList(),
                        status = ContainerInfo.ContainerStatus.HEALTHY,
                    ),
                )

            coEvery { mockPipeline.getContainerInfo(any()) } answers { containerInfos[firstArg()] }
            coEvery { mockPipeline.getContainerInfo() } returns containerInfos
            coJustRun { mockPipeline.injectConfig(any(), any()) }
            coJustRun { mockPipeline.start() }

            val mockBackend = mockk<Backend>()
            val capturedName = slot<String>()

            coEvery { mockBackend.createPipeline(capture(capturedName), any()) } returns mockPipeline
            coEvery { mockBackend.getConnectorIp(any()) } returns "127.0.0.1"
            every { mockPipeline.name } answers { capturedName.captured }

            val contractStore = ContractStore(objectMapper)
            val pipelineManager = PipelineManager(objectMapper, mockBackend)
            val pipelineController = PipelineController(contractStore, pipelineManager)

            contractStore.addContract(testContract)

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
                    pipelineController.apply { registerRoutes() }
                }
            }
            val testClient =
                createClient {
                    install(ContentNegotiation) {
                        jackson()
                    }
                }

            val resp =
                testClient.post("/pipeline") {
                    basicAuth("test", "test")
                    header("Content-Type", ContentType.Application.Json)
                    setBody(
                        PipelineCreateRequest(
                            target = "http://dataowner.com/document:1234",
                            contractId = "http://dataowner.com/policy:6163",
                            action = "http://www.w3.org/ns/odrl/2/use",
                        ),
                    )
                }

            val pipelineName = resp.body<PipelineCreateResponse>().pipelineName
            assertEquals(capturedName.captured, pipelineName)

            coVerify(exactly = 1) { mockPipeline.start() }
            coVerify(exactly = 1) { mockPipeline.injectConfig(0, match { c -> c.nextContainer == "test-container-2" }) }
            coVerify(exactly = 1) { mockPipeline.injectConfig(1, match { c -> c.nextContainer == null }) }

            coJustRun { mockPipeline.stop() }
            coJustRun { mockPipeline.remove() }

            testClient.delete("/pipeline/$pipelineName") {
                basicAuth("test", "test")
            }

            coVerify(exactly = 1) { mockPipeline.stop() }
            coVerify(exactly = 1) { mockPipeline.remove() }
        }

    @Test
    fun `Test the docker-based pipeline implementation`() =
        runTest(timeout = 30.hours) {
            // TODO: Fix this in CI
            //return@runTest

            // Since we do real io calls, we cannot use the virtual time provided by the kotlin coroutines test runner.
            // We don't want to use runBlocking, as it does not have the nice coroutine dumps os runTest, so here we are...
            withContext(Dispatchers.Default) {
                val dockerClient = DockerClient.fromEnv()
                assertTrue(dockerClient.runCatching { ping() }.isSuccess, "Cannot run test, docker cannot be pinged")

                // TODO: Once the image API is available, verify that the test images exist

                val dockerBackend = DockerBackend(objectMapper, dockerClient)
                val pipelineManager = PipelineManager(objectMapper, dockerBackend)

                pipelineManager.createPipeline(testContainers).use { pipeline ->
                    val containerInfo = pipeline.getContainerInfo()

                    // Technically, we should probably inspect the container here manually, but since the container info is
                    // made from the inspect result, that gets us nowhere.

                    val payload = Random.Default.nextBytes(32)

                    // Check the echo endpoint
                    val httpClient = HttpClient()
                    val resp =
                        httpClient.post("http://127.0.0.1:${containerInfo[0].ports[0].hostPort}/echo") {
                            setBody(payload)
                        }
                    assertTrue(payload.contentEquals(resp.body()))

                    assertTrue(pipelineManager.deletePipeline(pipeline.name))
                    delay(100)
                }
            }
        }

    companion object {
        private val objectMapper =
            jacksonMapperBuilder()
                .addModules(JsonLDModule(JsonLdOptions()), JSONPModule(), JavaTimeModule())
                .build()

        private val testContainers =
            GlueContainerPipeline(
                listOf(
                    GlueContainer(
                        image = "container-pipeline/proxy",
                        hash = "sha256:a4531b520e7053a1b43894e83d4bd53d9f350706c51e27fbc1aa54b4f2999890",
                        allowNetwork = true,
                    ),
                    GlueContainer(
                        image = "container-pipeline/http-echo",
                        hash = "sha256:1853980d5d73c3d1468fd86adeb1c3bc9ca5b32c96c661febfcce29720dba66f",
                    ),
                ),
            )

        private val testContract =
            File("test-containers/example-policy.json").reader().use { r ->
                OdrlParser().readOdrlPolicy(r)
            }
    }
}
