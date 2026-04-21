package de.fraunhofer.aisec.glue.pipeline.docker

import com.fasterxml.jackson.databind.ObjectMapper
import de.fraunhofer.aisec.glue.docker.Container
import de.fraunhofer.aisec.glue.docker.Network
import de.fraunhofer.aisec.glue.docker.model.ContainerState
import de.fraunhofer.aisec.glue.docker.model.EndpointSettings
import de.fraunhofer.aisec.glue.docker.model.PortBinding
import de.fraunhofer.aisec.glue.pipeline.*
import de.fraunhofer.iosb.odrl.OdrlPolicy
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.plugins.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds

class DockerContainerPipeline internal constructor(
    name: String,
    contractId: String,
    targetId: String,
    private val objectMapper: ObjectMapper,
    val containers: List<Container>,
    val networks: List<Network>,
    private val config: DockerBackend.DockerConfig,
) : ContainerPipeline(name, contractId, targetId) {
    private val closed = AtomicBoolean(false)
    private val followUpContracts: MutableMap<String, MutableSet<OdrlPolicy>> = mutableMapOf()
    private val contractsLock = Mutex()
    private val followUpPipelines = ConcurrentHashMap.newKeySet<RemotePipeline>()

    override suspend fun start() {
        for (container in containers) {
            container.start()
        }
    }

    private suspend fun stopContainer(container: Container) {
        try {
            withTimeout(5000.milliseconds) {
                container.stop()
            }
        } catch (_: TimeoutCancellationException) {
            LOG.warn { "Could not stop container ${container.name} within timeout" }
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
                LOG.atError {
                    message = "Could not remove container ${container.name}"
                    cause = e
                }
            }
        }

        for (network in networks) {
            network.runCatching {
                remove()
            }.onFailure { e ->
                LOG.atError {
                    message = "Could not remove network ${network.name}"
                    cause = e
                }
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
                "running" -> ContainerInfo.ContainerStatus.STARTED
                "created", "exited", "dead", "removing" -> ContainerInfo.ContainerStatus.DOWN
                // TODO: How to handle paused or restarting? Those are technically invalid for us but may still be reported.
                else -> throw IllegalStateException("The container returned an unexpected state: ${state.status}")
            }

        // TODO: If status is running, perform a health check (or check using the builtin docker HEALTHCHECK system?)

        return status
    }

    // TODO: Allow returning more than one.
    private fun parsePortBinding(
        name: String,
        bindings: List<PortBinding>,
    ): List<ContainerPort> {
        // Some input validation
        if (bindings.isEmpty()) {
            throw IllegalStateException("The provided port $name has no bindings")
        }
        val portComponents = name.split('/')
        if (portComponents.size != 2) {
            throw IllegalStateException("The port name $name has an invalid format")
        }
        val containerPort = portComponents[0].toUShort()
        val protocol = portComponents[1]

        // Since we use the port binding's addresses to connect to our containers from external hosts, we cannot use the
        // catch-all addresses for binding to all interfaces.
        // Instead, we allow the user to substitute an external, default address or domain name in the config.
        return bindings.map { b ->
            val hostIp =
                when (bindings[0].hostIp) {
                    "0.0.0.0", "::" -> config.publicHostname
                    else -> bindings[0].hostIp
                }

            ContainerPort(containerPort, "$hostIp:${b.hostPort}", protocol)
        }.distinct()
    }

    override suspend fun getContainerInfo(index: Int): ContainerInfo {
        return containers[index].let { c ->
            val inspect = c.inspect()

            ContainerInfo(
                name = c.name,
                // TODO: Use IPAMConfig instead
                ipAddresses = inspect.networkSettings?.networks?.values?.mapNotNull(EndpointSettings::ipAddress) ?: emptyList(),
                ports =
                    inspect.networkSettings?.ports?.flatMap {
                            (k, v) ->
                        v?.let { parsePortBinding(k, v) } ?: emptyList()
                    } ?: emptyList(),
                status = parseContainerStatus(inspect.state ?: throw IllegalStateException("Inspection has no state object")),
                pid = inspect.state.pid,
            )
        }
    }

    override suspend fun getContainerInfo(): List<ContainerInfo> =
        coroutineScope {
            containers.indices.map { async { getContainerInfo(it) } }.awaitAll()
        }

    override suspend fun injectConfig(
        index: Int,
        config: ContainerConfig,
    ) {
        containers[index].copyToContainer {
            writeFile("config.json", objectMapper.writeValueAsBytes(config))
        }
        LOG.atDebug {
            message = "Injected config into container $index"
            payload = mapOf("config" to config)
        }
    }

    override suspend fun addContract(
        peer: String,
        contract: OdrlPolicy,
    ): Boolean =
        contractsLock.withLock {
            // TODO: Add persistence here
            followUpContracts.getOrPut(peer, ::mutableSetOf).add(contract)
        }

    override suspend fun contracts(): Map<String, Set<OdrlPolicy>> =
        contractsLock.withLock {
            followUpContracts
        }

    override suspend fun contracts(peer: String): Set<OdrlPolicy> =
        contractsLock.withLock {
            followUpContracts[peer] ?: setOf()
        }

    override suspend fun addRemotePipeline(remotePipeline: RemotePipeline): Boolean {
        return followUpPipelines.add(remotePipeline)
    }

    override suspend fun remotePipelines(): Set<RemotePipeline> {
        return followUpPipelines
    }

    companion object {
        private val LOG = KotlinLogging.logger {}
    }
}
