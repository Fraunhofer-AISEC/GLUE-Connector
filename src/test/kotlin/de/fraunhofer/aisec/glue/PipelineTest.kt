/*
 * Copyright 2024-2026 Fraunhofer AISEC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.fraunhofer.aisec.glue

import de.fraunhofer.aisec.glue.atls.CMCClient
import de.fraunhofer.aisec.glue.config.ConfigManager
import de.fraunhofer.aisec.glue.controllers.PipelineController
import de.fraunhofer.aisec.glue.docker.DockerClient
import de.fraunhofer.aisec.glue.models.pipeline.PipelineCreateRequest
import de.fraunhofer.aisec.glue.models.pipeline.PipelineCreateResponse
import de.fraunhofer.aisec.glue.pipeline.*
import de.fraunhofer.aisec.glue.pipeline.docker.DockerBackend
import de.fraunhofer.aisec.glue.policy.PolicyEngine
import de.fraunhofer.iosb.odrl.glue.GlueContainer
import de.fraunhofer.iosb.odrl.glue.GlueContainerPipeline
import de.fraunhofer.iosb.odrl.parser.OdrlParser
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
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
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.io.TempDir
import java.net.InetAddress
import java.nio.file.Path
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

class PipelineTest {
    @TempDir
    lateinit var tempDir: Path

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
                        ports = listOf(ContainerPort(8080u.toUShort(), "127.0.0.1:12345", "tcp")),
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
            coEvery { mockPipeline.remotePipelines() } returns emptySet()
            coJustRun { mockPipeline.injectConfig(any(), any()) }
            coJustRun { mockPipeline.start() }

            val mockBackend = mockk<Backend>()
            val pipelineName = slot<String>()

            coEvery { mockBackend.createPipeline(capture(pipelineName), any(), any(), any()) } returns mockPipeline
            coEvery { mockBackend.getConnectorIp(any()) } returns "127.0.0.1"

            val mockRemotePipelineManager = mockk<RemotePipelineManager>()

            val mockCmcClient = mockk<CMCClient>()
            coEvery { mockCmcClient.tlsKey(any(), any(), any(), any(), any()) } returns "test-key"

            val pipelineManager =
                PipelineManager(mockRemotePipelineManager, mockBackend, mockCmcClient, ConfigManager(MapApplicationConfig()))

            val pipeline = pipelineManager.createPipeline(getTestContainers(), "testContract", "testTarget")

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
                        ports = listOf(ContainerPort(8080u.toUShort(), "127.0.0.1:12345", "tcp")),
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
            coEvery { mockPipeline.remotePipelines() } returns emptySet()
            coJustRun { mockPipeline.injectConfig(any(), any()) }
            coJustRun { mockPipeline.start() }

            val mockBackend = mockk<Backend>()
            val capturedName = slot<String>()

            coEvery { mockBackend.createPipeline(capture(capturedName), any(), any(), any()) } returns mockPipeline
            coEvery { mockBackend.getConnectorIp(any()) } returns "127.0.0.1"
            every { mockPipeline.name } answers { capturedName.captured }

            val mockCmcClient = mockk<CMCClient>()
            coEvery { mockCmcClient.tlsKey(any(), any(), any(), any(), any()) } returns "test-key"

            val mockRemotePipelineManager = mockk<RemotePipelineManager>()
            val dataStore = DataStore(objectMapper, tempDataStoreConfigManager(tempDir))
            val pipelineManager =
                PipelineManager(mockRemotePipelineManager, mockBackend, mockCmcClient, ConfigManager(MapApplicationConfig()))
            val policyEngine = PolicyEngine(pipelineManager)
            val pipelineController = PipelineController(dataStore, pipelineManager, policyEngine)

            dataStore.addContract(testContract)

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
                    pipelineController.apply { registerRoutes() }
                }
            }
            val testClient =
                createClient {
                    install(ClientContentNegotiation) {
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
            // Since we do real io calls, we cannot use the virtual time provided by the kotlin coroutines test runner.
            // We don't want to use runBlocking, as it does not have the nice coroutine dumps os runTest, so here we are...
            withContext(Dispatchers.Default) {
                val dockerClient = DockerClient.fromEnv()
                assertTrue(dockerClient.runCatching { ping() }.isSuccess, "Cannot run test, docker cannot be pinged")

                val httpClient = HttpClient()
                val remotePipelineManager = RemotePipelineManager(httpClient, objectMapper)
                val configManager =
                    ConfigManager(
                        MapApplicationConfig(
                            "docker.publicHostname" to (System.getenv("DOCKER_HOSTNAME") ?: InetAddress.getLocalHost().hostAddress),
                            // We don't have a CMC in the CI, so don't attempt to spawn aTLS proxies
                            "atls.enabled" to "false",
                        ),
                    )
                val dockerBackend = DockerBackend(objectMapper, dockerClient, configManager)

                val mockCmcClient = mockk<CMCClient>()
                coEvery { mockCmcClient.tlsKey(any(), any(), any(), any(), any()) } returns "test-key"

                val pipelineManager =
                    PipelineManager(remotePipelineManager, dockerBackend, mockCmcClient, ConfigManager(MapApplicationConfig()))

                LOG.info { "Test setup complete" }

                pipelineManager.createPipeline(getTestContainers(dockerClient), "testContract", "testTarget").use { pipeline ->
                    val containerInfo = pipeline.getContainerInfo()

                    val inspectResult = dockerClient.containers.get("pipeline-${pipeline.name}-0").inspect()
                    LOG.atInfo {
                        message = "Obtained docker inspect result and parsed container info"
                        payload =
                            mapOf(
                                "inspectResult" to inspectResult,
                                "containerInfo" to containerInfo,
                            )
                    }

                    val payload = Random.nextBytes(32)

                    // Check the echo endpoint
                    val hostAddress = containerInfo[0].ports[0].hostAddress
                    val resp =
                        assertDoesNotThrow("Exception when connecting to the deployed pipeline") {
                            httpClient.post("http://$hostAddress/echo") {
                                setBody(payload)
                            }
                        }
                    assertTrue(payload.contentEquals(resp.body()))

                    assertTrue(pipelineManager.deletePipeline(pipeline.name))
                    delay(100)
                }
            }
        }

    suspend fun getTestContainers(dockerClient: DockerClient? = null): GlueContainerPipeline {
        val proxyImage = System.getenv("PROXY_IMAGE_NAME") ?: "container-pipeline/proxy"
        val httpEchoImage = System.getenv("HTTP_ECHO_IMAGE_NAME") ?: "container-pipeline/http-echo"
        return GlueContainerPipeline(
            platformSecurity = listOf("tpm2"),
            container =
                listOf(
                    GlueContainer(
                        image = proxyImage,
                        hash = "sha256:${getContainerIdHash(proxyImage, dockerClient)}",
                        allowNetwork = true,
                    ),
                    GlueContainer(
                        image = httpEchoImage,
                        hash = "sha256:${getContainerIdHash(httpEchoImage, dockerClient)}",
                    ),
                ),
        )
    }

    suspend fun getContainerIdHash(
        image: String,
        dockerClient: DockerClient? = null,
    ): String {
        val imageToCheck = dockerClient?.images?.get(image) ?: return "invalid-hash"
        val inspectResponse = imageToCheck.inspect()
        return inspectResponse.id?.split(":")?.last()
            ?: throw IllegalStateException("No container ID hash found")
    }

    companion object {
        private val objectMapper = OdrlParser().mapper

        private val testContract = OdrlParser().readOdrlPolicy(rawExampleOffer.reader())

        private val LOG = KotlinLogging.logger {}
    }
}
