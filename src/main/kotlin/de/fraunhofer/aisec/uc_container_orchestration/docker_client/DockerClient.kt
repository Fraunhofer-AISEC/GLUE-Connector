package de.fraunhofer.aisec.uc_container_orchestration.docker_client

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.slf4j.LoggerFactory
import java.io.Closeable

class DockerClient(dockerSocket: String = "/var/run/docker.sock") : Closeable{
    internal val httpClient = HttpClient(OkHttp) {
        expectSuccess = true
        engine {
            config {
                socketFactory(UnixSocketFactory(dockerSocket))
            }
        }
        install(ContentNegotiation) {
            json(Json {
                // This allows us to only specify the keys we need when modeling json responses from docker
                // Alternatively, we could just generate the API with openapi and extract the required models
                ignoreUnknownKeys = true
            })
        }
    }

    override fun close() {
        httpClient.close()
    }

    suspend fun ping() {
        httpClient.head("$BASE_URL/_ping")
    }

    inner class Containers {
        suspend fun create(name: String, image: String, init: Boolean = false): Container {
            val args = JsonObject(
                mapOf(
                    "Image" to JsonPrimitive(image),
                    "HostConfig" to JsonObject(
                        mapOf(
                            "NetworkMode" to JsonPrimitive("none"),
                            "Init" to JsonPrimitive(init)
                        )
                    )
                )
            )
            httpClient.post("$BASE_URL/containers/create") {
                parameter("name", name)
                setBody(args)
                contentType(ContentType.Application.Json)
            }

            val container = Container(this@DockerClient, name)
            // Currently, we have to manually disconnect from the none network, so we can add our own networks afterward
            // There is an old issue regarding this: https://github.com/moby/moby/issues/21132
            this@DockerClient.networks.get("none").disconnect(container)
            return container
        }

        fun get(name: String): Container {
            return Container(this@DockerClient, name)
        }
    }
    val containers = Containers()

    inner class Networks {
        suspend fun create(name: String, subnet: String): Network {
            val args = JsonObject(mapOf(
                "Name" to JsonPrimitive(name),
                "Driver" to JsonPrimitive("bridge"),
                "Internal" to JsonPrimitive(true),
                "IPAM" to JsonObject(mapOf(
                    "Driver" to JsonPrimitive("default"),
                    "Config" to JsonArray(listOf(
                        JsonObject(mapOf(
                            "Subnet" to JsonPrimitive(subnet)
                        ))
                    ))
                ))
            ))

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