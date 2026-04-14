package de.fraunhofer.aisec.glue.docker

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import de.fraunhofer.aisec.glue.docker.model.*
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import java.io.Closeable
import java.net.ConnectException
import java.net.URI
import java.nio.file.Path
import kotlin.math.pow
import de.fraunhofer.aisec.glue.docker.model.Network as NetworkInspectResponse

class DockerClient(internal val httpClient: HttpClient, private val baseUrl: Url) : Closeable {
    init {
        // Monitor retry events for logging
        httpClient.monitor.subscribe(HttpRequestRetryEvent) { event ->
            val delayBase = 1.5
            val nextDelay = (delayBase * 1000 * delayBase.pow(event.retryCount.toDouble() - 1)).toLong().coerceAtMost(30000)
            LOG.atWarn {
                message = "Retrying Docker request"
                payload = mapOf("url" to event.request.url.toString(), "retryCount" to event.retryCount, "nextDelayMs" to nextDelay)
            }
        }
    }

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

        suspend fun list(): List<ContainerSummary> {
            return httpClient.get(endpoint("containers/json")) {
                contentType(ContentType.Application.Json)
            }.body()
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

        suspend fun create(req: NetworkCreateRequest): Network {
            httpClient.post(endpoint("networks/create")) {
                setBody(req)
                contentType(ContentType.Application.Json)
            }

            return Network(this@DockerClient, req.name)
        }

        fun get(name: String): Network {
            return Network(this@DockerClient, name)
        }

        @Suppress("unused")
        suspend fun inspectAll(): List<NetworkInspectResponse> {
            return httpClient.get(endpoint("networks")).body()
        }
    }

    val networks = Networks()

    inner class Images {
        suspend fun createByPull(name: String): Image {
            LOG.atDebug {
                message = "Attempting to pull the image from image registry"
                payload = mapOf("imageName" to name)
            }
            try {
                httpClient.post(endpoint("images/create")) {
                    parameter("fromImage", name)
                    System.getenv("REGISTRY_AUTH_TOKEN")?.let {
                        header("X-Registry-Auth", it)
                    }
                }
            } catch (e: ClientRequestException) {
                LOG.atWarn {
                    message = "Failed to pull image, will try local image"
                    payload = mapOf("imageName" to name)
                    cause = e
                }
            }
            return Image(this@DockerClient, name)
        }

        suspend fun get(name: String): Image {
            val image = Image(this@DockerClient, name)

            try {
                image.inspect()
            } catch (_: ClientRequestException) {
                return createByPull(name)
            }

            return image
        }
    }

    val images = Images()

    companion object {
        private val LOG = KotlinLogging.logger {}

        private const val API_VERSION = "v1.51"
        internal val UNIX_BASE_URL = Url("http://localhost/$API_VERSION")

        private val defaultConfig: HttpClientConfig<CIOEngineConfig>.() -> Unit = {
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
            install(HttpRequestRetry) {
                maxRetries = 10
                retryOnExceptionIf { _, cause ->
                    cause is ConnectException
                }
                exponentialDelay(base = 1.5, maxDelayMs = 30000)
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
                    if (!tlsVerify.isNullOrEmpty()) {
                        val certDir =
                            System.getenv(
                                "DOCKER_CERT_PATH",
                            ) ?: Path.of(System.getProperty("user.home"), ".docker").toString()

                        val (clientCertificate, trustManager) = TLSHelper.loadDockerTLSParams(certDir)
                        val dockerBaseUrl =
                            URLBuilder(uri.toString()).apply {
                                protocol = URLProtocol("https", 0)
                                // This looks a bit ugly, but should work to assign the correct default port for the
                                // docker https protocol. If we use ktor's default mechanism, then we'll have problems
                                // with port 443
                                if (port == 0) {
                                    port = 2376
                                }
                                appendPathSegments(API_VERSION)
                            }.build()

                        LOG.atInfo {
                            message = "Using docker engine TLS socket"
                            payload = mapOf("location" to dockerBaseUrl, "certDir" to certDir)
                        }
                        DockerClient(
                            HttpClient(CIO) {
                                defaultConfig()
                                engine {
                                    https {
                                        this.trustManager = trustManager
                                        certificates.add(clientCertificate)
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
                        LOG.atInfo {
                            message = "Using docker engine TCP socket"
                            payload = mapOf("location" to dockerBaseUrl)
                        }
                        // Plain HTTP should only be used locally for testing and when unix is not available
                        DockerClient(
                            HttpClient(CIO, defaultConfig),
                            dockerBaseUrl,
                        )
                    }
                }
                "unix" -> {
                    LOG.atInfo {
                        message = "Using docker engine unix socket"
                        payload = mapOf("location" to uri.path)
                    }
                    DockerClient(
                        HttpClient(CIO) {
                            defaultConfig()
                            defaultRequest {
                                unixSocket(uri.path)
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
