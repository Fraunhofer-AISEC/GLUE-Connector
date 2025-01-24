package de.fraunhofer.aisec.glue.pipeline.docker

import com.fasterxml.jackson.databind.ObjectMapper
import de.fraunhofer.aisec.glue.docker.Container
import de.fraunhofer.aisec.glue.docker.Network
import de.fraunhofer.aisec.glue.docker.model.ContainerState
import de.fraunhofer.aisec.glue.docker.model.EndpointSettings
import de.fraunhofer.aisec.glue.docker.model.PortBinding
import de.fraunhofer.aisec.glue.pipeline.ContainerConfig
import de.fraunhofer.aisec.glue.pipeline.ContainerInfo
import de.fraunhofer.aisec.glue.pipeline.ContainerPipeline
import de.fraunhofer.aisec.glue.pipeline.ContainerPort
import io.ktor.client.plugins.*
import io.ktor.http.*
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

class DockerContainerPipeline internal constructor(
    name: String,
    private val objectMapper: ObjectMapper,
    val containers: List<Container>,
    val networks: List<Network>,
) : ContainerPipeline(name) {
    private val closed = AtomicBoolean(false)

    override suspend fun start() {
        for (container in containers) {
            container.start()
        }
    }

    private suspend fun stopContainer(container: Container) {
        try {
            withTimeout(5000) {
                container.stop()
            }
        } catch (e: TimeoutCancellationException) {
            LOG.warn("Could not stop container ${container.name} within timeout")
            try {
                container.kill()
            } catch (e: ClientRequestException) {
                // Sometimes, the container gets stopped while we try to kill it during the timeout.
                // In that case, we get an exception here.
                if (e.response.status != HttpStatusCode.Conflict) {
                    throw e
                }
            }
        }
    }

    override suspend fun stop() {
        coroutineScope {
            for (container in containers) {
                launch {
                    stopContainer(container)
                }
            }
        }
    }

    private suspend fun removeImpl() {
        for (container in containers) {
            container.runCatching {
                remove(deleteVolumes = true, force = true)
            }.onFailure { e ->
                LOG.error("Could not remove container ${container.name}", e)
            }
        }

        for (network in networks) {
            network.runCatching {
                remove()
            }.onFailure { e ->
                LOG.error("Could not remove network ${network.name}", e)
            }
        }
    }

    override suspend fun remove() {
        if (closed.compareAndSet(false, true)) {
            removeImpl()
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            runBlocking { removeImpl() }
        }
    }

    private fun parseContainerStatus(state: ContainerState): ContainerInfo.ContainerStatus {
        val status =
            when (state.status) {
                "created" -> ContainerInfo.ContainerStatus.DOWN
                "running" -> ContainerInfo.ContainerStatus.STARTED
                "exited" -> ContainerInfo.ContainerStatus.DOWN
                "dead" -> ContainerInfo.ContainerStatus.DOWN
                // Removing requires a prior exit, so treat as down
                "removing" -> ContainerInfo.ContainerStatus.DOWN
                // TODO: How to handle paused or restarting? Those are technically invalid for us but may still be reported.
                else -> throw IllegalStateException("The container returned an unexpected state: ${state.status}")
            }

        // TODO: If status is running, perform a health check (or check using the builtin docker HEALTHCHECK system?)

        return status
    }

    private fun parsePortBinding(
        name: String,
        bindings: List<PortBinding>,
    ): ContainerPort {
        // Some input validation
        if (bindings.isEmpty()) {
            throw IllegalStateException("The provided port $name has no bindings")
        }
        // Before starting the container, the port is bound to 0
        val hostPort = bindings[0].hostPort?.toUShortOrNull() ?: 0u
        val portComponents = name.split('/')
        if (portComponents.size != 2) {
            throw IllegalStateException("The port name $name has an invalid format")
        }
        val containerPort = portComponents[0].toUShort()
        val protocol = portComponents[1]

        return ContainerPort(containerPort, hostPort, protocol)
    }

    override suspend fun getContainerInfo(index: Int): ContainerInfo? {
        return containers.getOrNull(index)?.let { c ->
            val inspect = c.inspect()

            ContainerInfo(
                name = c.name,
                ipAddresses = inspect.networkSettings?.networks?.values?.mapNotNull(EndpointSettings::ipAddress) ?: emptyList(),
                ports = inspect.networkSettings?.ports?.mapNotNull { (k, v) -> v?.let { parsePortBinding(k, v) } } ?: emptyList(),
                status = parseContainerStatus(inspect.state!!),
                pid = inspect.state.pid,
            )
        }
    }

    override suspend fun getContainerInfo(): List<ContainerInfo> {
        return containers.indices.map { i -> getContainerInfo(i)!! }
    }

    override suspend fun injectConfig(
        index: Int,
        config: ContainerConfig,
    ) {
        containers[index].copyToContainer {
            writeFile("config.json", objectMapper.writeValueAsBytes(config))
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(DockerContainerPipeline::class.java)
    }
}
