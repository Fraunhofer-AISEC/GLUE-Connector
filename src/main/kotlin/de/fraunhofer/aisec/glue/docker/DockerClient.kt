package de.fraunhofer.aisec.glue.docker

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import de.fraunhofer.aisec.glue.docker.model.ContainerCreateRequest
import de.fraunhofer.aisec.glue.docker.model.HostConfig
import de.fraunhofer.aisec.glue.docker.model.PortBinding
import de.fraunhofer.aisec.glue.docker.unix.UnixSocketFactory
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.net.URI
import java.nio.file.Path

class DockerClient(internal val httpClient: HttpClient, private val baseUrl: Url) : Closeable {
    /**
     * Docker constructor for backward compatibility only.
     * Please prefer [fromEnv] to ensure that different environments are picked up correctly.
     */
    constructor(dockerSocket: String = "/var/lib/docker.sock") :
        this(
            HttpClient(OkHttp) {
                defaultConfig()
                engine {
                    config {
                        socketFactory(UnixSocketFactory(dockerSocket))
                    }
                }
            },
            UNIX_BASE_URL,
        )

    /**
     * Convert a relative URL path to an absolute URL suitable for this docker client.
     */
    internal fun endpoint(path: String): Url {
        return URLBuilder(baseUrl).appendEncodedPathSegments(path).build()
    }

    override fun close() {
        httpClient.close()
    }

    suspend fun ping() {
        httpClient.head(endpoint("_ping"))
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
            httpClient.post(endpoint("containers/create")) {
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

            httpClient.post(endpoint("networks/create")) {
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

        private const val API_VERSION = "v1.43"
        private val UNIX_BASE_URL = Url("http://localhost/$API_VERSION")

        private val defaultConfig: HttpClientConfig<OkHttpConfig>.() -> Unit = {
            expectSuccess = true
            install(ContentNegotiation) {
                jackson(streamRequestBody = false) {
                    setSerializationInclusion(JsonInclude.Include.NON_NULL)
                    // Docker can apparently include fields from newer protocol revisions, which we should ignore
                    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                }
            }
            install(HttpTimeout) {
                // The docker daemon will wait for some time before killing a container if it refuses to stop
                // If 30s is too long, the application will need to enforce its own timeout
                socketTimeoutMillis = 30000
            }
        }

        fun fromEnv(): DockerClient {
            val envVar = System.getenv("DOCKER_HOST") ?: "unix:///var/run/docker.sock"
            val uri = URI(envVar)

            return when (uri.scheme) {
                // Note: http and https are actually not supported by docker but its more intuitive to allow them as
                // docker over tcp uses http(s) internally
                "tcp" -> {
                    val tlsVerify = System.getenv("DOCKER_TLS_VERIFY")
                    if (tlsVerify != null && tlsVerify != "") {
                        val certDir =
                            System.getenv(
                                "DOCKER_CERT_PATH",
                            ) ?: Path.of(System.getProperty("user.home"), ".docker").toString()

                        val (sslContext, trustManager) = TLSHelper.loadDockerTLSParams(certDir)
                        val dockerBaseUrl =
                            URLBuilder(uri.toString()).apply {
                                protocol = URLProtocol("https", 0)
                                // This looks a bit ugly, but should work to assign the correct default port for the docker
                                // https protocol. If we use ktor's default mechanism then we'll have problems with port 443
                                if (port == 0) {
                                    port = 2376
                                }
                                appendPathSegments(API_VERSION)
                            }.build()

                        LOG.info("Using docker engine TLS socket at $dockerBaseUrl with certificates from $certDir")
                        DockerClient(
                            HttpClient(OkHttp) {
                                defaultConfig()
                                engine {
                                    config {
                                        sslSocketFactory(sslContext.socketFactory, trustManager)
                                    }
                                }
                            },
                            dockerBaseUrl,
                        )
                    } else {
                        val dockerBaseUrl =
                            URLBuilder(uri.toString()).apply {
                                protocol = URLProtocol("http", 0)
                                if (port == 0) {
                                    port = 2375
                                }
                                appendPathSegments(API_VERSION)
                            }.build()
                        LOG.info("Using docker engine TCP socket at $dockerBaseUrl")
                        // Plain HTTP should only be used locally for testing and when unix is not available
                        DockerClient(
                            HttpClient(OkHttp, defaultConfig),
                            dockerBaseUrl,
                        )
                    }
                }
                "unix" -> {
                    LOG.info("Using docker engine unix socket at ${uri.path}")
                    DockerClient(
                        HttpClient(OkHttp) {
                            defaultConfig()
                            engine {
                                config {
                                    socketFactory(UnixSocketFactory(uri.path))
                                }
                            }
                        },
                        UNIX_BASE_URL,
                    )
                }

                else -> throw IllegalArgumentException("Protocol ${uri.scheme} is not supported")
            }
        }
    }
}
