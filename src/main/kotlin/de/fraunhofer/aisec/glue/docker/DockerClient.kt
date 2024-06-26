package de.fraunhofer.aisec.glue.docker

import de.fraunhofer.aisec.glue.docker.model.ContainerCreateRequest
import de.fraunhofer.aisec.glue.docker.model.HostConfig
import de.fraunhofer.aisec.glue.docker.model.PortBinding
import de.fraunhofer.aisec.glue.docker.unix.UnixSocketFactory
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import org.slf4j.LoggerFactory
import java.io.Closeable

class DockerClient(dockerSocket: String = "/var/run/docker.sock") : Closeable {
    internal val httpClient =
        HttpClient(OkHttp) {
            expectSuccess = true
            engine {
                config {
                    socketFactory(UnixSocketFactory(dockerSocket))
                }
            }
            install(ContentNegotiation) {
            }
            install(HttpTimeout) {
                // The docker daemon will wait for some time before killing a container if it refuses to stop
                // If 30s is too long, the application will need to enforce its own timeout
                socketTimeoutMillis = 30000
            }
        }

    override fun close() {
        httpClient.close()
    }

    suspend fun ping() {
        httpClient.head("$BASE_URL/_ping")
    }

    inner class Containers {
        suspend fun create(
            name: String,
            image: String,
            entrypoint: List<String>? = null,
            cmd: List<String>? = null,
            ports: Map<Int, Int>? = null,
            env: Map<String, String>? = null,
        ): Container {
            val req =
                ContainerCreateRequest(
                    image = image,
                    env = env?.map { (k, v) -> "$k=$v" },
                    entrypoint = entrypoint,
                    cmd = cmd,
                    exposedPorts = ports?.keys?.associate { p -> Pair("$p/tcp", mapOf<String, Any>()) },
                    hostConfig =
                        HostConfig(
                            portBindings =
                                ports?.toList()?.associate { (k, v) ->
                                    Pair(
                                        "$k/tcp",
                                        listOf(
                                            PortBinding(hostIp = "::", hostPort = "$v"),
                                            PortBinding(hostIp = "0.0.0.0", hostPort = "$v"),
                                        ),
                                    )
                                },
                        ),
                )

            return create(name, req)
        }

        suspend fun create(
            name: String,
            req: ContainerCreateRequest,
        ): Container {
            httpClient.post("$BASE_URL/containers/create") {
                parameter("name", name)
                setBody(req)
                contentType(ContentType.Application.Json)
            }

            val container = Container(this@DockerClient, name)
            // Currently, we have to manually disconnect from the none network, so we can add our own networks afterward
            // There is an old issue regarding this: https://github.com/moby/moby/issues/21132
            when (req.hostConfig?.networkMode) {
                "none" -> this@DockerClient.networks.get("none").disconnect(container)
            }
            return container
        }

        fun get(name: String): Container {
            return Container(this@DockerClient, name)
        }
    }

    val containers = Containers()

    inner class Networks {
        suspend fun create(
            name: String,
            subnet: String,
        ): Network {
            val args =
                mapOf(
                    "Name" to name,
                    "Driver" to "bridge",
                    "Internal" to true,
                    "IPAM" to
                        mapOf(
                            "Driver" to "default",
                            "Config" to
                                listOf(
                                    mapOf(
                                        "Subnet" to subnet,
                                    ),
                                ),
                        ),
                )

            httpClient.post("$BASE_URL/networks/create") {
                setBody(args)
                contentType(ContentType.Application.Json)
            }

            return Network(this@DockerClient, name)
        }

        fun get(name: String): Network {
            return Network(this@DockerClient, name)
        }
    }

    val networks = Networks()

    companion object {
        private val LOG = LoggerFactory.getLogger(DockerClient::class.java)
        internal const val BASE_URL = "http://localhost/v1.43"
    }
}
