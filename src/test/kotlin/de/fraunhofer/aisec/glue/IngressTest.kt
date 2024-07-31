package de.fraunhofer.aisec.glue

import de.fraunhofer.aisec.glue.docker.model.ContainerInspectResponse
import de.fraunhofer.aisec.glue.docker.model.EndpointSettings
import de.fraunhofer.aisec.glue.docker.model.NetworkSettings
import de.fraunhofer.aisec.glue.models.Container
import de.fraunhofer.aisec.glue.models.Contract
import de.fraunhofer.aisec.glue.models.Pipeline
import de.fraunhofer.aisec.glue.proxy.Proxy
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.singleton
import kotlin.test.Test
import de.fraunhofer.aisec.glue.docker.Container as DockerContainer

class IngressTest {
    @Test
    fun `Ingress requires authentication`() =
        testApplication {
            val resp = client.get("/contract/42/call")
            assert(resp.status == HttpStatusCode.Unauthorized)
            assert(resp.headers["WWW-Authenticate"] != null)
        }

    private fun setupFakeContainerPipeline(): Pair<ContractManager, ContainerManager> {
        val mockContract =
            Contract(
                Pipeline(
                    "",
                    listOf(
                        Container("test", "debian", listOf()),
                    ),
                ),
            )

        val mockContractManager = mockk<ContractManager>()
        every { mockContractManager.get("test") } returns mockContract

        val mockContainer = mockk<DockerContainer>()
        coEvery { mockContainer.inspect() } returns
            ContainerInspectResponse(
                id = "42",
                name = "test",
                networkSettings =
                    NetworkSettings(
                        networks =
                            mapOf(
                                "link-0" to
                                    EndpointSettings(
                                        ipAddress = "10.10.42.2",
                                    ),
                            ),
                    ),
            )

        val mockPipeline: ContainerPipeline = mockk<ContainerPipeline>()
        every { mockPipeline.containers } returns listOf(mockContainer)

        val mockContainerManager = mockk<ContainerManager>()
        coEvery { mockContainerManager.pipelineForContract(mockContract) } returns mockPipeline

        return Pair(mockContractManager, mockContainerManager)
    }

    @Test
    fun `Ingress can forward requests`() =
        testApplication {
            val (mockContractManager, mockContainerManager) = setupFakeContainerPipeline()

            val mockProxy = mockk<Proxy>()
            coEvery { mockProxy.delegate(any(), any()) } returns (object : OutgoingContent.NoContent() {})

            environment {
                // Clear modules config option as we set this ourselves
                config = MapApplicationConfig(listOf())
            }

            application {
                module(
                    DI {
                        import(defaultDependencies)

                        bind<ContractManager>(overrides = true) with singleton { mockContractManager }
                        bind<ContainerManager>(overrides = true) with singleton { mockContainerManager }
                        bind<Proxy>(overrides = true) with singleton { mockProxy }
                    },
                )
            }

            val resp =
                client.get("/contract/test/call/test") {
                    // TODO: somehow make authentication pluggable
                    //        Use DI as well or run a test OAuth server (overkill)?
                    basicAuth("user", "password")
                }

            assert(resp.status.isSuccess())

            coVerify { mockProxy.delegate(any(), "http://10.10.42.2:8080/test") }
        }
}
